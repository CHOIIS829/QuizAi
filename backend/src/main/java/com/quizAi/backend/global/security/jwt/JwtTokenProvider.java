package com.quizAi.backend.global.security.jwt;

import com.quizAi.backend.domain.member.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.auth.jwt-secret}")
    private String jwtSecret;

    @Value("${app.auth.access-token-seconds}")
    private long accessTokenSeconds;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenSeconds);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("nickname", user.getNickname())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public AuthenticatedUser getAuthentication(String token) {
        Claims claims = parseClaims(token);
        return new AuthenticatedUser(
                Long.parseLong(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("nickname", String.class)
        );
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
