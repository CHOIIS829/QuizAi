package com.quizAi.backend.global.security.oauth;

import com.quizAi.backend.domain.member.entity.OAuthProvider;
import com.quizAi.backend.domain.member.entity.User;
import com.quizAi.backend.domain.member.service.MemberService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final MemberService memberService;

    public CustomOAuth2UserService(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo oAuth2UserInfo = extractUserInfo(registrationId, oAuth2User);
        if (!StringUtils.hasText(oAuth2UserInfo.email())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("oauth_email_missing"),
                    "이메일 정보를 가져오지 못했습니다."
            );
        }

        User user = memberService.getOrCreateOAuthUser(
                OAuthProvider.fromRegistrationId(registrationId),
                oAuth2UserInfo
        );

        return new QuizOAuth2User(user, oAuth2User.getAttributes());
    }

    @SuppressWarnings("unchecked")
    private OAuth2UserInfo extractUserInfo(String registrationId, OAuth2User oAuth2User) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return new OAuth2UserInfo(
                    stringValue(oAuth2User.getAttributes(), "sub"),
                    stringValue(oAuth2User.getAttributes(), "email"),
                    stringValue(oAuth2User.getAttributes(), "picture")
            );
        }

        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        Map<String, Object> profile = kakaoAccount == null ? Map.of() : (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());

        return new OAuth2UserInfo(
                stringValue(oAuth2User.getAttributes(), "id"),
                stringValue(kakaoAccount, "email"),
                stringValue(profile, "profile_image_url")
        );
    }

    private String stringValue(Map<String, Object> attributes, String key) {
        if (attributes == null) {
            return null;
        }
        Object value = attributes.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
