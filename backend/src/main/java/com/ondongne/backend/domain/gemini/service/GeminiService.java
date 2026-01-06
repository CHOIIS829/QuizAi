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

//%

    public QuizResponseDto generateQuizFromText(String text, int count) {
        log.info(">>>>> Gemini Text Request Start. Length: {}", text.length());

        String prompt = "다음 텍스트 내용을 분석하여 학습용 퀴즈를 만들어줘.";
        GeminiRequestDto.Part contentPart = GeminiRequestDto.Part.builder().text(text).build();

        return callGeminiApi(prompt, contentPart, count);
    }

    public QuizResponseDto generateQuizFromVideo(String filePath, int count) {
        log.info(">>>>> Gemini Video Request Start. File: {}", filePath);

        String fileUri = uploadVideo(filePath);
        log.info(">>>>> 비디오 업로드 완료. File URI: {}", fileUri);

        waitForProcessing(fileUri);

        String prompt = "업로드된 비디오의 시청각 정보를 분석하여 학습용 퀴즈를 만들어줘. " +
                "특히 화면에 나오는 코드나 도표가 중요하다면 timestamp와 boundingBox를 포함해줘.";

        GeminiRequestDto.Part contentPart = GeminiRequestDto.Part.builder()
                .fileData(new GeminiRequestDto.FileData("video/mp4", fileUri))
                .build();

        return callGeminiApi(prompt, contentPart, count);
    }

    private QuizResponseDto callGeminiApi(String userPrompt, GeminiRequestDto.Part contentPart, int count) {
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
                  "boundingBox": [0.1, 0.2, 0.5, 0.6]
                }
              ]
            }
            timestamp와 boundingBox는 비디오 분석 시 시각적 자료가 중요할 때만 포함해.
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

        // [수정 포인트 1] 문자열 합치기 + URI.create() 사용
        // UriComponentsBuilder를 쓰지 않고 직접 문자열을 조합하여 인코딩 이슈를 원천 차단합니다.
        // trim()을 추가하여 혹시 모를 공백 문자 제거
        String urlString = String.format("%s/%s:generateContent?key=%s",
                GEMINI_BASE_URL, modelName.trim(), apiKey.trim());

        URI uri = URI.create(urlString);

        log.info(">>>>> Calling Gemini API URI: {}", uri);

        try {
            GeminiResponseDto response = webClientBuilder.build()
                    .post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponseDto.class)
                    .block();

            if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
                throw new RuntimeException("Gemini API 응답이 비어있습니다.");
            }
            String jsonText = response.getCandidates().get(0).getContent().getParts().get(0).getText();
            return objectMapper.readValue(jsonText, QuizResponseDto.class);

        } catch (WebClientResponseException e) {
            // [수정 포인트 2] 구글 서버가 보낸 실제 에러 메시지 확인
            log.error(">>>>> Gemini API 호출 오류: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Gemini API 호출 중 오류 발생: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error(">>>>> Gemini 응답 파싱 또는 기타 오류", e);
            throw new RuntimeException("AI 응답을 처리할 수 없습니다.");
        }
    }

    private String uploadVideo(String localFilePath) {
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
        return (String) fileInfo.get("uri");
    }

    private void waitForProcessing(String fileUri) {
        WebClient client = webClientBuilder.build();
        String fileId = fileUri.substring(fileUri.lastIndexOf("/") + 1);

        // 상태 확인 URL도 직접 조합
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
