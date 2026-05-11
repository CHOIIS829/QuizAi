package com.quizAi.backend.domain.member.service;

import com.quizAi.backend.domain.member.dto.CurrentUserDto;
import com.quizAi.backend.domain.member.entity.OAuthAccount;
import com.quizAi.backend.domain.member.entity.OAuthProvider;
import com.quizAi.backend.domain.member.entity.User;
import com.quizAi.backend.domain.member.entity.UserStatus;
import com.quizAi.backend.domain.member.repository.OAuthAccountRepository;
import com.quizAi.backend.domain.member.repository.UserRepository;
import com.quizAi.backend.global.exception.DuplicateNicknameException;
import com.quizAi.backend.global.exception.ResourceNotFoundException;
import com.quizAi.backend.global.security.jwt.AuthenticatedUser;
import com.quizAi.backend.global.security.oauth.OAuth2UserInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class MemberService {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;

    public MemberService(UserRepository userRepository, OAuthAccountRepository oAuthAccountRepository) {
        this.userRepository = userRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
    }

    @Transactional
    public User getOrCreateOAuthUser(OAuthProvider provider, OAuth2UserInfo oAuth2UserInfo) {
        return oAuthAccountRepository.findByProviderAndProviderUserId(provider, oAuth2UserInfo.providerUserId())
                .map(OAuthAccount::getUser)
                .map(user -> {
                    updateUserProfile(user, oAuth2UserInfo.profileImageUrl());
                    return touchLogin(user);
                })
                .orElseGet(() -> createOrLinkUser(provider, oAuth2UserInfo));
    }

    @Transactional(readOnly = true)
    public User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("회원을 찾을 수 없습니다.", "USER_NOT_FOUND"));
    }

    @Transactional(readOnly = true)
    public CurrentUserDto getCurrentUser(AuthenticatedUser authenticatedUser) {
        User user = getRequiredUser(authenticatedUser.getUserId());
        return CurrentUserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .needsNickname(!StringUtils.hasText(user.getNickname()))
                .build();
    }

    @Transactional
    public CurrentUserDto updateNickname(Long userId, String nickname) {
        String normalizedNickname = nickname.trim();
        userRepository.findByNickname(normalizedNickname)
                .filter(existingUser -> !existingUser.getId().equals(userId))
                .ifPresent(existingUser -> {
                    throw new DuplicateNicknameException();
                });

        User user = getRequiredUser(userId);
        user.updateNickname(normalizedNickname);
        return getCurrentUser(AuthenticatedUser.from(user));
    }

    private User createOrLinkUser(OAuthProvider provider, OAuth2UserInfo oAuth2UserInfo) {
        User user = userRepository.findByEmail(oAuth2UserInfo.email())
                .map(existingUser -> {
                    updateUserProfile(existingUser, oAuth2UserInfo.profileImageUrl());
                    return touchLogin(existingUser);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(oAuth2UserInfo.email())
                        .profileImageUrl(oAuth2UserInfo.profileImageUrl())
                        .status(UserStatus.ACTIVE)
                        .lastLoginAt(LocalDateTime.now())
                        .build()));

        oAuthAccountRepository.save(OAuthAccount.builder()
                .user(user)
                .provider(provider)
                .providerUserId(oAuth2UserInfo.providerUserId())
                .providerEmail(oAuth2UserInfo.email())
                .build());

        return user;
    }

    private User touchLogin(User user) {
        user.updateLastLogin(LocalDateTime.now());
        return user;
    }

    private void updateUserProfile(User user, String profileImageUrl) {
        if (StringUtils.hasText(profileImageUrl)) {
            user.updateProfileImageUrl(profileImageUrl);
        }
    }
}
