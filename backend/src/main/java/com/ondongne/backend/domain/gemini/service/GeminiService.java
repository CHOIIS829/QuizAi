package com.ondongne.backend.domain.gemini.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondongne.backend.domain.gemini.dto.GeminiRequestDto;
import com.ondongne.backend.domain.gemini.dto.GeminiResponseDto;
import com.ondongne.backend.domain.quiz.dto.QuizResponseDto;
import com.ondongne.backend.domain.quiz.dto.QuizResultDto;
import com.ondongne.backend.global.exception.GeminiFailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
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

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final String UPLOAD_API_URL = "https://generativelanguage.googleapis.com/upload/v1beta";

    public void generateQuizFromText(String text, int count) {
        log.info(">>>>> Gemini Text Request Start. Length: {}", text.length());

        String prompt = "제공된 텍스트의 핵심 내용을 심층 분석하여, 중요한 개념을 검증할 수 있는 고품질의 학습용 퀴즈를 만들어줘.";
        GeminiRequestDto.Part contentPart = GeminiRequestDto.Part.builder().text(text).build();

        callGeminiApi(prompt, contentPart, count)
                .subscribe(
                        result -> {
                            log.info(">>>>> Gemini 호출 성공. 생성된 퀴즈 개수: {}", result.getQuestions().size());

                        },
                        error -> {
                            log.error(">>>>> Gemini 호출 실패: {}", error.getMessage());

                        }
                );
    }

    public void generateQuizFromVideo(String filePath, int count) {
        log.info(">>>>> Gemini Video Request Start. File: {}", filePath);

        // 1. 업로드
        String fileUri = uploadVideo(filePath);
        log.info(">>>>> 비디오 업로드 완료. File URI: {}", fileUri);

        // 2. 처리 대기
        waitForProcessing(fileUri);

        String prompt = "업로드된 비디오의 시청각 정보를 심층 분석하여, 중요한 개념을 검증할 수 있는 고품질의 학습용 퀴즈를 만들어줘.";

        GeminiRequestDto.Part contentPart = GeminiRequestDto.Part.builder()
                .fileData(new GeminiRequestDto.FileData("video/mp4", fileUri))
                .build();

        // 3. 퀴즈 생성
        callGeminiApi(prompt, contentPart, count);

    }

private Mono<QuizResultDto> callGeminiApi(String userPrompt, GeminiRequestDto.Part contentPart, int count) {
        String systemPrompt = String.format("""
            너는 IT 기술 학습을 돕는 '모의고사 생성기'야.
            반드시 다음 JSON 구조를 준수해서 %d개의 객관식 문제를 출제해.
            응답은 Markdown 포맷 없이 순수 JSON 문자열만 반환해야 해.
            
            JSON 구조 예시:
            {
              "title": "주제 제목",
              "questions": [
                {
                  "id": 1,
                  "question": "문제 지문",
                  "options": ["보기1", "보기2", "보기3", "보기4"],
                  "answer": "보기1",
                  "explanation": "해설",
                  "codeSnippet": "필요시 코드 스니펫"
                }
              ]
            }
            각 문제는 다음 조건을 반드시 따라야 해.
            - 문제의 지문은 명확하고 간결하게 작성.
            - 동영상이나 택스트를 안본 사람도 지식을 가지고 있으면 풀 수 있을 정도로 만들어줘.
            - 정답은 반드시 options에 포함된 보기 중 하나여야 해.
            - 출제된 문제들은 모두 서로 다른 유형이어야 해.
            - codeSnippet 필드는 코드가 포함된 문제에만 작성하고, 그렇지 않으면 빈 문자열로 둬.
            - codeSnippet 은 무조건 문제를 푸는 데 필요한 최소한의 코드만 포함해야 해.
            - 절대 응답 형식을 벗어나지 말고, JSON 형식에 맞지 않는 어떠한 설명도 추가하지 마.
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
                .onStatus(status -> status.isError(), clientResponse ->
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
                        return Mono.error(new GeminiFailException());
                    }
                })
                .doOnError(e -> log.error(">>>>> Gemini API 호출: {}", e.getMessage()));
    }

    private Mono<String> uploadVideo(String localFilePath) {

        return Mono.fromCallable(() -> {
            File file = new File(localFilePath);
            if (!file.exists()) {
                throw new RuntimeException("파일을 찾을 수 없습니다: " + localFilePath);
            }
            return file;
        }).flatMap(file -> {
            long numBytes = file.length();
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
                    .map(entity -> entity.getHeaders().getFirst("X-Goog-Upload-URL"))
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

    private void waitForProcessing(String fileUri) {

        WebClient client = webClientBuilder.build();
        String fileId = fileUri.substring(fileUri.lastIndexOf("/") + 1);

        String statusUrlString = String.format("https://generativelanguage.googleapis.com/v1beta/files/%s?key=%s", fileId, apiKey.trim());
        URI checkStatusUri = URI.create(statusUrlString);

        int maxRetries = 120;

        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(2000);

                Map response = client.get()
                        .uri(checkStatusUri)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                String state = (String) response.get("state");
                log.info(">>>>> Video Processing State: {}", state);

                if ("ACTIVE".equals(state)) {
                    return;
                } else if ("FAILED".equals(state)) {
                    throw new RuntimeException("비디오 처리에 실패했습니다.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("대기 중 인터럽트 발생");
            }
        }
        throw new RuntimeException("비디오 처리 시간이 초과되었습니다.");
    }

}
