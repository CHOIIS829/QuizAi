package com.quizAi.backend.global.security.oauth;

public record OAuth2UserInfo(
        String providerUserId,
        String email,
        String profileImageUrl
) {
}
