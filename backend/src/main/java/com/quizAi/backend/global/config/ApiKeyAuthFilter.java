package com.quizAi.backend.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Value("${app.auth.api-key}")
    private String validApiKey;

    private static final String API_KEY_HEADER = "X-API-KEY";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // CORS 프리플라이트 (OPTIONS) 요청은 인증을 거치지 않고 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 헬스 체크 등 공개 경로는 통과 (필요 시 수정)
        String path = request.getRequestURI();
        if (path.startsWith("/actuator/health") || path.startsWith("/favicon.ico") || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestApiKey = request.getHeader(API_KEY_HEADER);

        if (validApiKey.equals(requestApiKey)) {
            // [인증] API Key가 유효하면 Spring Security 컨텍스트에 인증 객체 등록
            var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    "api-key-user", null, java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_API_USER")));
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            
            filterChain.doFilter(request, response);
        } else {
            log.warn(">>>>> [AUTH ERROR] Invalid API Key from IP: {}", request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 401, \"message\": \"유효하지 않은 API Key입니다.\", \"errorCode\": \"INVALID_API_KEY\"}");
        }
    }
}
