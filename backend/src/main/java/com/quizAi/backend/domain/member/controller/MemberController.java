package com.quizAi.backend.domain.member.controller;

import com.quizAi.backend.domain.member.dto.CurrentUserDto;
import com.quizAi.backend.domain.member.dto.NicknameUpdateRequestDto;
import com.quizAi.backend.domain.member.service.MemberService;
import com.quizAi.backend.global.response.SuccessResponse;
import com.quizAi.backend.global.security.jwt.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/nickname")
    public ResponseEntity<SuccessResponse<CurrentUserDto>> updateNickname(
            Authentication authentication,
            @Valid @RequestBody NicknameUpdateRequestDto requestDto
    ) {
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        CurrentUserDto currentUserDto = memberService.updateNickname(authenticatedUser.getUserId(), requestDto.getNickname());

        return ResponseEntity.ok(SuccessResponse.<CurrentUserDto>builder()
                .code(200)
                .message("Success")
                .data(currentUserDto)
                .build());
    }
}
