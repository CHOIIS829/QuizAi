package com.quizAi.backend.global.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieUtils {

    @Value("${app.auth.access-cookie-name}")
    private String accessCookieName;

    @Value("${app.auth.refresh-cookie-name}")
    private String refreshCookieName;

    @Value("${app.auth.access-token-seconds}")
    private long accessTokenSeconds;

    @Value("${app.auth.refresh-token-seconds}")
    private long refreshTokenSeconds;

    @Value("${app.auth.cookie-secure}")
    private boolean secureCookie;

    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(accessCookieName, token, accessTokenSeconds).toString());
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(refreshCookieName, token, refreshTokenSeconds).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(accessCookieName, "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(refreshCookieName, "", 0).toString());
    }

    public String extractAccessToken(HttpServletRequest request) {
        return extractCookieValue(request, accessCookieName);
    }

    public String extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, refreshCookieName);
    }

    private ResponseCookie buildCookie(String cookieName, String value, long maxAgeSeconds) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }

        Optional<Cookie> cookie = Arrays.stream(request.getCookies())
                .filter(item -> cookieName.equals(item.getName()))
                .findFirst();

        return cookie.map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .orElse(null);
    }
}
