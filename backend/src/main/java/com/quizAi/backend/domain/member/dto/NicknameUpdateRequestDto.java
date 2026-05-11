package com.quizAi.backend.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class NicknameUpdateRequestDto {

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9_]+$", message = "닉네임은 한글, 영문, 숫자, 밑줄(_)만 사용할 수 있습니다.")
    private String nickname;
}
