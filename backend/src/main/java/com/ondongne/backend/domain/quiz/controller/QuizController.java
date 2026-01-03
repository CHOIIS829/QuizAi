package com.ondongne.backend.domain.quiz.controller;

import com.ondongne.backend.domain.quiz.dto.QuizRequestDto;
import com.ondongne.backend.domain.quiz.service.QuizService;
import com.ondongne.backend.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;
    private final RestClient.Builder builder;

    @PostMapping("/generate")
    public ResponseEntity<SuccessResponse<String>> generateQuiz(@RequestBody QuizRequestDto quizRequestDto) {

        String processComplete = quizService.processQuiz(quizRequestDto.getUrl(), quizRequestDto.getQuizCount());

        return ResponseEntity.ok(SuccessResponse.<String>builder()
                .code(200)
                .message("Success")
                .data(processComplete)
                .build());
    }
}
