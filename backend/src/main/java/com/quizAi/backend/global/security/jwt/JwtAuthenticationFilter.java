package com.quizAi.backend.global.security.jwt;

import com.quizAi.backend.domain.member.entity.User;
import com.quizAi.backend.domain.member.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtils cookieUtils;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            CookieUtils cookieUtils,
            UserRepository userRepository
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.cookieUtils = cookieUtils;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken = cookieUtils.extractAccessToken(request);
        String refreshToken = cookieUtils.extractRefreshToken(request);

        try {
            if (StringUtils.hasText(accessToken)) {
                setAuthentication(jwtTokenProvider.getAuthentication(accessToken));
            } else if (canRefresh(request) && StringUtils.hasText(refreshToken)) {
                refreshAuthentication(response, refreshToken);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            if (canRefresh(request) && StringUtils.hasText(refreshToken)) {
                if (!refreshAuthentication(response, refreshToken)) {
                    cookieUtils.clearAuthCookies(response);
                }
            } else {
                cookieUtils.clearAuthCookies(response);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean canRefresh(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !"/api/auth/logout".equals(path);
    }

    private boolean refreshAuthentication(HttpServletResponse response, String refreshToken) {
        Optional<Long> userId = refreshTokenService.findUserIdByToken(refreshToken);
        if (userId.isEmpty()) {
            return false;
        }

        Optional<User> user = userRepository.findById(userId.get());
        if (user.isEmpty()) {
            refreshTokenService.revoke(refreshToken);
            return false;
        }

        refreshTokenService.revoke(refreshToken);

        String newAccessToken = jwtTokenProvider.createAccessToken(user.get());
        String newRefreshToken = refreshTokenService.issueToken(user.get().getId());

        cookieUtils.addAccessTokenCookie(response, newAccessToken);
        cookieUtils.addRefreshTokenCookie(response, newRefreshToken);

        setAuthentication(AuthenticatedUser.from(user.get()));
        return true;
    }

    private void setAuthentication(AuthenticatedUser authenticatedUser) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
