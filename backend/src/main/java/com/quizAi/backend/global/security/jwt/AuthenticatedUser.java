package com.quizAi.backend.global.security.jwt;

import com.quizAi.backend.domain.member.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AuthenticatedUser {

    private final Long userId;
    private final String email;
    private final String nickname;

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getNickname());
    }
}
