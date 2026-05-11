package com.quizAi.backend.domain.member.repository;

import com.quizAi.backend.domain.member.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByNickname(String nickname);
}
