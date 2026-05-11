package com.quizAi.backend.domain.quiz.controller;

import com.quizAi.backend.domain.quiz.dto.QuizDetailDto;
import com.quizAi.backend.domain.quiz.dto.QuizListItemDto;
import com.quizAi.backend.domain.quiz.entity.SourceType;
import com.quizAi.backend.domain.quiz.service.PersistedQuizService;
import com.quizAi.backend.global.response.PageResponseDto;
import com.quizAi.backend.global.response.SuccessResponse;
import com.quizAi.backend.global.security.jwt.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PersistedQuizController {

    private final PersistedQuizService persistedQuizService;

    @GetMapping("/api/my/quizzes")
    public ResponseEntity<SuccessResponse<PageResponseDto<QuizListItemDto>>> getMyQuizzes(
            Authentication authentication,
            @RequestParam(required = false) SourceType sourceType,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        PageResponseDto<QuizListItemDto> result = persistedQuizService.getMyQuizzes(
                authenticatedUser.getUserId(),
                sourceType,
                tag,
                page,
                size
        );

        return ResponseEntity.ok(SuccessResponse.<PageResponseDto<QuizListItemDto>>builder()
                .code(200)
                .message("Success")
                .data(result)
                .build());
    }

    @GetMapping("/api/board/quizzes")
    public ResponseEntity<SuccessResponse<PageResponseDto<QuizListItemDto>>> getBoardQuizzes(
            @RequestParam(required = false) SourceType sourceType,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        PageResponseDto<QuizListItemDto> result = persistedQuizService.getBoardQuizzes(sourceType, tag, page, size);

        return ResponseEntity.ok(SuccessResponse.<PageResponseDto<QuizListItemDto>>builder()
                .code(200)
                .message("Success")
                .data(result)
                .build());
    }

    @GetMapping("/api/quizzes/{quizId}")
    public ResponseEntity<SuccessResponse<QuizDetailDto>> getQuizDetail(
            @PathVariable Long quizId,
            Authentication authentication
    ) {
        Long requesterId = authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser
                ? authenticatedUser.getUserId()
                : null;

        QuizDetailDto result = persistedQuizService.getQuizDetail(requesterId, quizId);

        return ResponseEntity.ok(SuccessResponse.<QuizDetailDto>builder()
                .code(200)
                .message("Success")
                .data(result)
                .build());
    }
}
