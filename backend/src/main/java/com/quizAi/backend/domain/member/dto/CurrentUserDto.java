package com.quizAi.backend.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurrentUserDto {

    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private boolean needsNickname;
}
