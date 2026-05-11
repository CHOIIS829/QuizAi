package com.quizAi.backend.global.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String errorCode = "oauth_login_failed";
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            errorCode = oauthException.getError().getErrorCode();
        }

        String errorMessage = exception.getMessage() == null ? "OAuth login failed" : exception.getMessage();
        log.warn("OAuth login failed. code={}, message={}", errorCode, errorMessage);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/auth/callback")
                .queryParam("error", errorCode)
                .queryParam("reason", errorMessage)
                .build()
                .encode()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
