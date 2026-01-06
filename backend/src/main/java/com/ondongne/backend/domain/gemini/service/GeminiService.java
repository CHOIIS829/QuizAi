package com.ondongne.backend.domain.gemini.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondongne.backend.domain.gemini.dto.GeminiRequestDto;
import com.ondongne.backend.domain.gemini.dto.GeminiResponseDto;
import com.ondongne.backend.domain.quiz.dto.QuizResponseDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

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

    public QuizResponseDto generateQuizFromText(String text, int count) {
        log.info(">>>>> Gemini Text Request Start. Length: {}", text.length());
        long startTime = System.currentTimeMillis(); // [시간 측정] 시작

        String prompt = "다음 텍스트 내용을 분석하여 학습용 퀴즈를 만들어줘.";
        GeminiRequestDto.Part contentPart = GeminiRequestDto.Part.builder().text(text).build();

        QuizResponseDto response = callGeminiApi(prompt, contentPart, count);

        long endTime = System.currentTimeMillis(); // [시간 측정] 종료
        log.info(">>>>> 텍스트 퀴즈 생성 완료. 총 소요 시간: {}ms", (endTime - startTime));

        return response;
    }

    public QuizResponseDto generateQuizFromVideo(String filePath, int count) {
        log.info(">>>>> Gemini Video Request Start. File: {}", filePath);
        long startTime = System.currentTimeMillis(); // [시간 측정] 전체 시작

        // 1. 업로드 (시간 측정 포함됨)
        String fileUri = uploadVideo(filePath);
        log.info(">>>>> 비디오 업로드 완료. File URI: {}", fileUri);

        // 2. 처리 대기 (시간 측정 포함됨)
        waitForProcessing(fileUri);

        String prompt = "업로드된 비디오의 시청각 정보를 분석하여 학습용 퀴즈를 만들어줘. " +
                "특히 화면에 나오는 코드나 도표가 중요하다면 timestamp도 함께 알려줘.";

        GeminiRequestDto.Part contentPart = GeminiRequestDto.Part.builder()
                .fileData(new GeminiRequestDto.FileData("video/mp4", fileUri))
                .build();

        // 3. 퀴즈 생성 (시간 측정 포함됨)
        QuizResponseDto response = callGeminiApi(prompt, contentPart, count);

        long endTime = System.currentTimeMillis(); // [시간 측정] 전체 종료
        log.info(">>>>> 비디오 퀴즈 생성 전체 완료. 총 소요 시간: {}ms", (endTime - startTime));

        return response;
    }

    private QuizResponseDto callGeminiApi(String userPrompt, GeminiRequestDto.Part contentPart, int count) {
        // ... (프롬프트 구성 코드는 동일하여 생략) ...
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
                  "timestamp": "05:23", 
                }
              ]
            }
            timestamp는 비디오 분석 시 시각적 자료가 중요할 때만 포함해.
            모든 문제에 대해서 시작적자료가 필요하지는 않아.
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
                        .build())
                .build();

        String urlString = String.format("%s/%s:generateContent?key=%s",
                GEMINI_BASE_URL, modelName.trim(), apiKey.trim());
        URI uri = URI.create(urlString);

        log.info(">>>>> Calling Gemini API URI: {}", uri);

        long startTime = System.currentTimeMillis(); // [시간 측정] API 호출 시작

        try {
            GeminiResponseDto response = webClientBuilder.build()
                    .post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponseDto.class)
                    .block();

            long endTime = System.currentTimeMillis(); // [시간 측정] API 응답 수신
            log.info(">>>>> Gemini API 응답 수신 완료. 소요 시간: {}ms", (endTime - startTime));

            if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
                throw new RuntimeException("Gemini API 응답이 비어있습니다.");
            }
            String jsonText = response.getCandidates().get(0).getContent().getParts().get(0).getText();
            return objectMapper.readValue(jsonText, QuizResponseDto.class);

        } catch (WebClientResponseException e) {
            log.error(">>>>> Gemini API 호출 오류: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Gemini API 호출 중 오류 발생: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error(">>>>> Gemini 응답 파싱 또는 기타 오류", e);
            throw new RuntimeException("AI 응답을 처리할 수 없습니다.");
        }
    }

    private String uploadVideo(String localFilePath) {
        long startTime = System.currentTimeMillis(); // [시간 측정] 업로드 시작

        File file = new File(localFilePath);
        if (!file.exists()) {
            throw new RuntimeException("파일을 찾을 수 없습니다: " + localFilePath);
        }
        long numBytes = file.length();
        String mimeType = "video/mp4";

        WebClient client = webClientBuilder.build();

        Map<String, Object> metadata = Map.of(
                "file", Map.of("display_name", file.getName())
        );

        String uploadUrlString = String.format("%s/files?key=%s", UPLOAD_API_URL, apiKey.trim());
        URI startUploadUri = URI.create(uploadUrlString);

        // 1단계: 업로드 세션 시작 요청
        String uploadUrl = client.post()
                .uri(startUploadUri)
                .header("X-Goog-Upload-Protocol", "resumable")
                .header("X-Goog-Upload-Command", "start")
                .header("X-Goog-Upload-Header-Content-Length", String.valueOf(numBytes))
                .header("X-Goog-Upload-Header-Content-Type", mimeType)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(metadata)
                .retrieve()
                .toEntity(String.class)
                .map(entity -> entity.getHeaders().getFirst("X-Goog-Upload-URL"))
                .block();

        if (uploadUrl == null) {
            throw new RuntimeException("Upload URL을 받아오지 못했습니다.");
        }

        // 2단계: 실제 파일 데이터 전송
        Map response = client.post()
                .uri(URI.create(uploadUrl))
                .header("X-Goog-Upload-Command", "upload, finalize")
                .header("X-Goog-Upload-Offset", "0")
                .contentType(MediaType.parseMediaType(mimeType))
                .body(BodyInserters.fromResource(new FileSystemResource(file)))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("file")) {
            throw new RuntimeException("파일 업로드 응답이 올바르지 않습니다.");
        }

        Map<String, Object> fileInfo = (Map<String, Object>) response.get("file");
        String fileUri = (String) fileInfo.get("uri");

        long endTime = System.currentTimeMillis(); // [시간 측정] 업로드 완료
        log.info(">>>>> 파일 업로드 프로세스 완료. 소요 시간: {}ms", (endTime - startTime));

        return fileUri;
    }

    private void waitForProcessing(String fileUri) {
        long startTime = System.currentTimeMillis(); // [시간 측정] 대기 시작

        WebClient client = webClientBuilder.build();
        String fileId = fileUri.substring(fileUri.lastIndexOf("/") + 1);

        String statusUrlString = String.format("https://generativelanguage.googleapis.com/v1beta/files/%s?key=%s", fileId, apiKey.trim());
        URI checkStatusUri = URI.create(statusUrlString);

        int maxRetries = 30;

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
                    long endTime = System.currentTimeMillis(); // [시간 측정] 처리 완료
                    log.info(">>>>> 비디오 처리 완료(ACTIVE). 대기 시간: {}ms", (endTime - startTime));
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
