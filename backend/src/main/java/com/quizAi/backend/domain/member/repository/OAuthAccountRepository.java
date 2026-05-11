package com.quizAi.backend.domain.member.repository;

import com.quizAi.backend.domain.member.entity.OAuthAccount;
import com.quizAi.backend.domain.member.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
}
