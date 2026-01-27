package com.ondongne.backend.domain.gemini.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondongne.backend.domain.gemini.dto.GeminiRequestDto;
import com.ondongne.backend.domain.gemini.dto.GeminiResponseDto;
import com.ondongne.backend.domain.quiz.dto.QuizResponseDto;
import com.ondongne.backend.domain.quiz.dto.QuizResultDto;
import com.ondongne.backend.domain.quiz.repository.JobRedisRepository;
import com.ondongne.backend.global.exception.GeminiFailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.model}")
    private String modelName;

    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final JobRedisRepository jobRedisRepository;

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final String UPLOAD_API_URL = "https://generativelanguage.googleapis.com/upload/v1beta";

    public void generateQuizFromText(String jobId, String text, int count) {
        log.info(">>>>> Gemini Text Request Start. Length: {}", text.length());

        String prompt = "제공된 텍스트의 핵심 내용을 심층 분석하여, 중요한 개념을 검증할 수 있는 고품질의 학습용 퀴즈를 만들어줘.";
        GeminiRequestDto.Part contentPart = GeminiRequestDto.Part.builder().text(text).build();

        callGeminiApi(prompt, contentPart, count)
                .subscribe(
                        result -> {
                            log.info("Job {} 완료. Redis 저장 중...", jobId);
                            jobRedisRepository.update(jobId, QuizResponseDto.JobStatus.COMPLETED, "퀴즈 생성이 완료되었습니다.", result);
                        },
                        error -> {
                            log.error("Job {} 실패. 에러: {}", jobId, error.getMessage());
                            jobRedisRepository.update(jobId, QuizResponseDto.JobStatus.FAILED, "퀴즈 생성에 실패했습니다.", null);
                        }
                );
    }

    public void generateQuizFromVideo(String jobId, String filePath, int count) {
        log.info(">>>>> Gemini Video Request Start. File: {}", filePath);

        uploadVideoAsync(filePath)
                .doFinally(signalType -> deleteLocalFile(filePath)) // [추가] 업로드 종료(성공/실패) 후 즉시 파일 삭제
                .flatMap(fileUri -> {
                    log.info(">>>>> [Job: {}] 업로드 완료. URI: {}. 처리 대기 시작...", jobId, fileUri);
                    // 2. 비동기 처리 대기 (Processing 상태 확인)
                    return waitForProcessingAsync(fileUri).thenReturn(fileUri);
                })
                .flatMap(fileUri -> {
                    log.info(">>>>> [Job: {}] 처리 완료 (ACTIVE). 퀴즈 생성 요청...", jobId);

                    // 3. 퀴즈 생성 요청 (프롬프트 및 요청 객체 준비)
                    String prompt = "업로드된 비디오의 시청각 정보를 심층 분석하여, 중요한 개념을 검증할 수 있는 고품질의 학습용 퀴즈를 만들어줘.";

                    GeminiRequestDto.Part contentPart = GeminiRequestDto.Part.builder()
                            .fileData(new GeminiRequestDto.FileData("video/mp4", fileUri))
                            .build();

                    // 이전에 리팩토링한 callGeminiApi 호출 (Mono 리턴)
                    return callGeminiApi(prompt, contentPart, count);
                })
                .subscribe(
                        // 성공 시 Redis 저장
                        quizResult -> {
                            log.info(">>>>> [Job: {}] 퀴즈 생성 성공! Redis 저장 중...", jobId);
                            jobRedisRepository.update(jobId, QuizResponseDto.JobStatus.COMPLETED, "퀴즈 생성이 완료되었습니다.", quizResult);
                        },
                        // 실패 시 에러 저장
                        error -> {
                            log.error(">>>>> [Job: {}] 처리 중 실패: {}", jobId, error.getMessage(), error);
                            jobRedisRepository.update(jobId, QuizResponseDto.JobStatus.FAILED, "퀴즈 생성에 실패했습니다.", null);
                        }
                );
    }

private Mono<QuizResultDto> callGeminiApi(String userPrompt, GeminiRequestDto.Part contentPart, int count) {
    String systemPrompt = String.format("""
        너는 IT 기술 학습을 돕는 숙련된 '모의고사 출제자'야.
        제공된 내용을 심층 분석하여 학습자가 내용을 완벽히 이해했는지 검증할 수 있는 수준 높은 객관식 문제 %d개를 출제해.
        
        [1. 문제 내용 및 품질 규칙] (★기존 요구사항 반영)
        - **지문 품질**: 문제는 명확하고 간결해야 하며, 모호한 표현을 피할 것.
        - **지식 기반**: 제공된 자료(영상/텍스트)를 보지 않았더라도, 해당 IT 개념을 알고 있는 사람이라면 풀 수 있는 '보편적 지식'을 묻는 문제여야 해. (단순한 영상 내용 기억력 테스트 금지)
        - **유형 다양성**: 단순 정의 묻기뿐만 아니라, 코드 분석, 상황 판단, 장단점 비교 등 서로 다른 유형의 문제들을 섞어서 출제해.
        - **정답 보장**: 정답은 반드시 제공된 4개의 보기(options) 안에 포함되어야 해.
        
        [2. 형식 및 기술적 제약 사항]
        - **출력 형식**: 오직 순수한 JSON 문자열만 반환해. (Markdown 코드 블록(```json)이나 불필요한 서론/결론 절대 금지)
        - **언어**: 모든 내용은 '한국어'로 작성해.
        - **보기 개수**: 모든 문제의 보기(options)는 정확히 4개씩 제공해.
        
        [3. 코드 스니펫 작성 규칙] (★정답 유출 방지)
        - 코드가 필요한 문제에만 `codeSnippet`을 작성하고, 불필요하면 빈 문자열("")로 둬.
        - `codeSnippet`은 문제를 푸는 데 필요한 최소한의 코드만 포함해.
        - **핵심 규칙**: 만약 문제의 정답이 코드의 특정 부분(메서드명, 키워드 등)이라면, 해당 부분은 절대 코드에 노출하지 마.
        - 대신 그 자리를 '_____' (밑줄 5개)로 대체하여 빈칸 채우기 문제로 만들어.
        - 예시: 정답이 `filter`라면, 코드는 `.filter(...)`가 아니라 `._____(...)`로 작성해야 해.
        
        [4. JSON 구조 예시]
        {
          "title": "주제 제목",
          "questions": [
            {
              "id": 1,
              "question": "다음 스트림 API 코드의 빈칸에 들어갈 알맞은 중개 연산은?",
              "options": ["map", "filter", "sorted", "limit"],
              "answer": "filter",
              "explanation": "조건에 맞는 요소만 걸러내기 위해서는 filter를 사용합니다.",
              "codeSnippet": "list.stream()._____(x -> x > 10).collect(Collectors.toList());"
            }
          ]
        }
    """, count);

        GeminiRequestDto request = GeminiRequestDto.builder()
                .systemInstruction(GeminiRequestDto.SystemInstruction.builder()
                        .parts(Collections.singletonList(GeminiRequestDto.Part.builder().text(systemPrompt).build()))
                        .build())
                .contents(Collections.singletonList(GeminiRequestDto.Content.builder()
                        .role("user")
                        .parts(List.of(contentPart, GeminiRequestDto.Part.builder().text(userPrompt).build()))
                        .build()))
                .generationConfig(GeminiRequestDto.GenerationConfig.builder()
                        .responseMimeType("application/json")
                        .temperature(0.85)
                        .build())
                .build();

        String urlString = String.format("%s/%s:generateContent?key=%s",
                GEMINI_BASE_URL, modelName.trim(), apiKey.trim());
        URI uri = URI.create(urlString);

        log.info(">>>>> Calling Gemini API URI: {}", uri);

        return webClientBuilder.build()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new GeminiFailException()))
                )
                .bodyToMono(GeminiResponseDto.class)
                .filter(response -> response.getCandidates() != null && !response.getCandidates().isEmpty())
                .switchIfEmpty(Mono.error(new GeminiFailException()))
                .flatMap(response -> {
                    try {
                        String jsonText = response.getCandidates().get(0).getContent().getParts().get(0).getText();
                        return Mono.just(objectMapper.readValue(jsonText, QuizResultDto.class));
                    } catch (Exception e) {
                        log.error(">>>>> Gemini 응답 파싱 오류: {}", e.getMessage());
                        return Mono.error(new GeminiFailException(e));
                    }
                })
                .doOnError(e -> log.error(">>>>> Gemini API 호출: {}", e.getMessage()));
    }

    private Mono<String> uploadVideoAsync(String localFilePath) {

        return Mono.fromCallable(() -> {
            File file = new File(localFilePath);
            if (!file.exists()) {
                throw new RuntimeException("파일을 찾을 수 없습니다: " + localFilePath);
            }
            long numBytes = file.length();
            return Map.of("file", file, "length", numBytes);
        }).subscribeOn(Schedulers.boundedElastic()).flatMap(data -> {
            File file = (File) data.get("file");
            long numBytes = (Long) data.get("length");
            String mimeType = "video/mp4";

            // 메타데이터
            Map<String, Object> metadata = Map.of("file", Map.of("display_name", file.getName()));
            String uploadUrlString = String.format("%s/files?key=%s", UPLOAD_API_URL, apiKey.trim());

            // 1단계: 업로드 세션 시작 (URL 받기)
            return webClientBuilder.build()
                    .post()
                    .uri(URI.create(uploadUrlString))
                    .header("X-Goog-Upload-Protocol", "resumable")
                    .header("X-Goog-Upload-Command", "start")
                    .header("X-Goog-Upload-Header-Content-Length", String.valueOf(numBytes))
                    .header("X-Goog-Upload-Header-Content-Type", mimeType)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(metadata)
                    .retrieve()
                    .toEntity(String.class)
                    .mapNotNull(entity -> entity.getHeaders().getFirst("X-Goog-Upload-URL"))
                    .flatMap(uploadUrl -> {
                        // 2단계: 실제 파일 바이트 전송
                        return webClientBuilder.build()
                                .post()
                                .uri(URI.create(uploadUrl))
                                .header("X-Goog-Upload-Command", "upload, finalize")
                                .header("X-Goog-Upload-Offset", "0")
                                .contentType(MediaType.parseMediaType(mimeType))
                                .body(BodyInserters.fromResource(new FileSystemResource(file)))
                                .retrieve()
                                .bodyToMono(Map.class);
                    })
                    .map(response -> {
                        Map<String, Object> fileInfo = (Map<String, Object>) response.get("file");
                        return (String) fileInfo.get("uri"); // 최종 File URI 반환
                    });
        });
    }

    private Mono<Void> waitForProcessingAsync(String fileUri) {

        String fileId = fileUri.substring(fileUri.lastIndexOf("/") + 1);
        String statusUrlString = String.format("https://generativelanguage.googleapis.com/v1beta/files/%s?key=%s", fileId, apiKey.trim());
        URI checkStatusUri = URI.create(statusUrlString);

        // Mono.defer를 써야 구독할 때마다 API를 새로 호출함
        return Mono.defer(() -> webClientBuilder.build()
                        .get()
                        .uri(checkStatusUri)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .flatMap(response -> {
                            String state = (String) response.get("state");
                            log.debug(">>>>> Video State Check: {}", state);

                            if ("ACTIVE".equals(state)) {
                                return Mono.empty(); // 성공! (반복 종료)
                            } else if ("FAILED".equals(state)) {
                                return Mono.error(new RuntimeException("Gemini 비디오 처리 실패 (FAILED)"));
                            } else {
                                // PROCESSING 상태면 에러를 던져서 retryWhen이 잡게 함
                                return Mono.error(new ProcessingNotFinishedException());
                            }
                        }))
                // ★ 핵심: Thread.sleep 대신 사용하는 리액티브 재시도 로직
                .retryWhen(Retry.fixedDelay(120, Duration.ofSeconds(2)) // 2초 간격, 최대 120번 시도
                        .filter(throwable -> throwable instanceof ProcessingNotFinishedException) // '아직 처리중'일 때만 재시도
                        .doBeforeRetry(retrySignal -> log.info(">>>>> 처리 중... 재시도 횟수: {}", retrySignal.totalRetries()))
                )
                .then(); // 결과값은 필요 없으니 Void로 변환
    }

    private void deleteLocalFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                if (file.delete()) {
                    log.info(">>>>> 로컬 임시 파일 삭제 완료: {}", filePath);
                } else {
                    log.warn(">>>>> 로컬 임시 파일 삭제 실패: {}", filePath);
                }
            }
        } catch (Exception e) {
            log.warn(">>>>> 로컬 파일 삭제 중 에러 무시: {}", e.getMessage());
        }
    }

    private static class ProcessingNotFinishedException extends RuntimeException {}

}
