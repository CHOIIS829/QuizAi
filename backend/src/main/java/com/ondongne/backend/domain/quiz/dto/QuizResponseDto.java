package com.ondongne.backend.domain.quiz.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponseDto {

    private String title;
    private List<QuestionDto> questions;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDto {
        private int id;
        private String question;
        private List<String> options;  // 4지 선다 보기
        private String answer;         // 정답
        private String explanation;    // 해설

        // --- 비디오 전용 필드 ---
//        private String timestamp;      // 타임스탬프 (예: "05:23")
//        private List<Float> boundingBox; // [ymin, xmin, ymax, xmax]
    }
}
