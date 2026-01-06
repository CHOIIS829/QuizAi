package com.ondongne.backend.domain.gemini.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
public class GeminiRequestDto {

    /**
     * 시스템 지시문(System Instructions)입니다.
     * AI에게 부여할 역할(Persona)이나 절대적인 규칙을 설정할 때 사용합니다. (예: "너는 퀴즈 생성기야")
     */
    private SystemInstruction systemInstruction;

    /**
     * 대화 내용이나 질문(Prompt)을 담는 리스트입니다.
     * 필수 항목이며, 사용자의 질문(user)과 모델의 답변(model)이 순서대로 쌓입니다.
     */
    private List<Content> contents;

    /**
     * 생성 설정(Generation Config)입니다.
     * 응답 포맷(JSON 강제), 창의성 조절(Temperature) 등의 기술적 설정을 담당합니다.
     */
    private GenerationConfig generationConfig;

    @Getter
    @Setter
    @Builder
    public static class SystemInstruction {
        private List<Part> parts;
    }

    @Getter
    @Setter
    @Builder
    public static class Content {
        /**
         * 화자의 역할입니다.
         * - "user": 사용자 (질문자)
         * - "model": AI (답변자)
         */
        private String role;

        /**
         * 메시지의 실제 내용물입니다.
         * 텍스트와 미디어(이미지, 비디오)를 혼합하여 보낼 수 있어 리스트 형태입니다.
         */
        private List<Part> parts;
    }

    @Getter
    @Setter
    @Builder
    public static class GenerationConfig {
        /**
         * 응답 MIME 타입 설정
         * - "application/json": AI가 잡담 없이 순수 JSON 포맷으로만 응답하도록 강제합니다.
         */
        private String responseMimeType;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Part {
        /**
         * 일반 텍스트 데이터 (질문 내용 등)
         */
        private String text;

        /**
         * 멀티미디어 파일 정보 (비디오, 이미지 등)
         * *주의: 파일 자체가 아니라 구글 서버에 업로드된 파일의 URI 정보를 담습니다.*
         */
        private FileData fileData;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileData {
        /**
         * 파일의 MIME 타입 (예: "video/mp4", "image/jpeg")
         */
        private String mimeType;
        /**
         * 구글 File API를 통해 업로드 후 발급받은 고유 식별자 URI입니다.
         * (로컬 파일 경로가 아닙니다.)
         */
        private String fileUri;
    }

}
