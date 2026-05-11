package com.quizAi.backend.global.security.oauth;

import com.quizAi.backend.domain.member.entity.User;
import com.quizAi.backend.global.security.jwt.CookieUtils;
import com.quizAi.backend.global.security.jwt.JwtTokenProvider;
import com.quizAi.backend.global.security.jwt.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtils cookieUtils;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            CookieUtils cookieUtils
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.cookieUtils = cookieUtils;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        QuizOAuth2User principal = (QuizOAuth2User) authentication.getPrincipal();
        User user = principal.getUser();

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = refreshTokenService.issueToken(user.getId());

        cookieUtils.addAccessTokenCookie(response, accessToken);
        cookieUtils.addRefreshTokenCookie(response, refreshToken);

        response.sendRedirect(frontendUrl + "/auth/callback");
    }
}
