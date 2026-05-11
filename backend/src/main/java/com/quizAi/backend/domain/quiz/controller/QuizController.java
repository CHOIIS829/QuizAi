package com.quizAi.backend.domain.quiz.controller;

import com.quizAi.backend.domain.quiz.dto.QuizRequestDto;
import com.quizAi.backend.domain.quiz.dto.QuizResponseDto;
import com.quizAi.backend.domain.quiz.service.QuizService;
import com.quizAi.backend.global.security.jwt.AuthenticatedUser;
import com.quizAi.backend.global.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/generate")
    public ResponseEntity<SuccessResponse<QuizResponseDto>> generateQuiz(
            @Valid @RequestBody QuizRequestDto quizRequestDto,
            Authentication authentication,
            HttpServletRequest request
    ) {

        Long ownerUserId = authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser
                ? authenticatedUser.getUserId()
                : null;
        QuizResponseDto quizResponseDto = quizService.processQuiz(
                quizRequestDto.getUrl(),
                quizRequestDto.getQuizCount(),
                ownerUserId,
                resolveClientIp(request)
        );

        return ResponseEntity.ok(SuccessResponse.<QuizResponseDto>builder()
                .code(200)
                .message("Success")
                .data(quizResponseDto)
                .build());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<SuccessResponse<QuizResponseDto>> getQuizStatus(@PathVariable String jobId) {

        QuizResponseDto quizResponseDto = quizService.getQuizStatus(jobId);

        return ResponseEntity.ok(SuccessResponse.<QuizResponseDto>builder()
                .code(200)
                .message("Success")
                .data(quizResponseDto)
                .build());
    }
}
