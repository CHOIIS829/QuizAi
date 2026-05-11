package com.quizAi.backend.domain.member.controller;

import com.quizAi.backend.domain.member.dto.CurrentUserDto;
import com.quizAi.backend.domain.member.service.MemberService;
import com.quizAi.backend.domain.quiz.dto.GuestQuizImportRequestDto;
import com.quizAi.backend.domain.quiz.dto.PersistedQuizResponseDto;
import com.quizAi.backend.domain.quiz.entity.QuizCreatedVia;
import com.quizAi.backend.domain.quiz.service.PersistedQuizService;
import com.quizAi.backend.global.response.SuccessResponse;
import com.quizAi.backend.global.security.jwt.AuthenticatedUser;
import com.quizAi.backend.global.security.jwt.CookieUtils;
import com.quizAi.backend.global.security.jwt.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final PersistedQuizService persistedQuizService;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtils cookieUtils;

    @GetMapping("/me")
    public ResponseEntity<SuccessResponse<CurrentUserDto>> getCurrentUser(Authentication authentication) {
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        CurrentUserDto currentUserDto = memberService.getCurrentUser(authenticatedUser);

        return ResponseEntity.ok(SuccessResponse.<CurrentUserDto>builder()
                .code(200)
                .message("Success")
                .data(currentUserDto)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtils.extractRefreshToken(request);
        refreshTokenService.revoke(refreshToken);
        cookieUtils.clearAuthCookies(response);

        return ResponseEntity.ok(SuccessResponse.<Void>builder()
                .code(200)
                .message("Success")
                .build());
    }

    @PostMapping("/guest-quizzes/import")
    public ResponseEntity<SuccessResponse<PersistedQuizResponseDto>> importGuestQuiz(
            Authentication authentication,
            @Valid @RequestBody GuestQuizImportRequestDto requestDto
    ) {
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        Long quizId = persistedQuizService.persistQuiz(
                authenticatedUser.getUserId(),
                requestDto.getSourceUrl(),
                requestDto.getQuizResult(),
                QuizCreatedVia.GUEST_IMPORTED
        );

        return ResponseEntity.ok(SuccessResponse.<PersistedQuizResponseDto>builder()
                .code(200)
                .message("Success")
                .data(PersistedQuizResponseDto.builder().quizId(quizId).build())
                .build());
    }
}
