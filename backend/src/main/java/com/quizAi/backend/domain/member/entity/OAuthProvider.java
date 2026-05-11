package com.quizAi.backend.domain.member.entity;

public enum OAuthProvider {
    GOOGLE,
    KAKAO;

    public static OAuthProvider fromRegistrationId(String registrationId) {
        return OAuthProvider.valueOf(registrationId.toUpperCase());
    }
}
