package com.ondongne.backend.domain.gemini.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class GeminiResponseDto {

    /**
     * AI가 생성한 답변 후보군
     */
    private List<Candidate> candidates;

    /**
     * 토큰 사용량 메타데이터 (비용 계산용)
     * 예: { "promptTokenCount": 10, "candidatesTokenCount": 20, "totalTokenCount": 30 }
     */
    private UsageMetadata usageMetadata;

    @Getter
    @Setter
    public static class Candidate {
        private Content content;
        private String finishReason; // "STOP", "MAX_TOKENS" 등 종료 사유
        private Double avgLogprobs;  // 확률 정보 (선택적)
    }

    @Getter
    @Setter
    public static class Content {
        private String role;
        private List<Part> parts;
    }

    @Getter
    @Setter
    public static class Part {
        private String text;
    }

    @Getter
    @Setter
    public static class UsageMetadata {
        private int promptTokenCount;     // 질문(입력)에 사용된 토큰 수
        private int candidatesTokenCount; // 답변(출력) 생성에 사용된 토큰 수
        private int totalTokenCount;      // 총 합계
    }
}
