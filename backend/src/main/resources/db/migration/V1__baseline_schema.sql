CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    nickname VARCHAR(30) NULL,
    profile_image_url VARCHAR(500) NULL,
    status ENUM('ACTIVE') NOT NULL,
    last_login_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_nickname UNIQUE (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE oauth_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider ENUM('GOOGLE', 'KAKAO') NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    provider_email VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_oauth_accounts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE topic_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(50) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_topic_tags_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quizzes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    source_url TINYTEXT NOT NULL,
    source_type ENUM('BLOG', 'WEB', 'YOUTUBE') NOT NULL,
    source_host VARCHAR(255) NOT NULL,
    visibility ENUM('PRIVATE', 'PUBLIC') NOT NULL,
    created_via ENUM('GUEST_IMPORTED', 'MEMBER_GENERATED') NOT NULL,
    published_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_quizzes_owner FOREIGN KEY (owner_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    quiz_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    question TINYTEXT NOT NULL,
    options_json TEXT NOT NULL,
    answer VARCHAR(255) NOT NULL,
    explanation TINYTEXT NOT NULL,
    code_snippet TINYTEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_quiz_questions_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quiz_topic_tags (
    quiz_id BIGINT NOT NULL,
    topic_tag_id BIGINT NOT NULL,
    PRIMARY KEY (quiz_id, topic_tag_id),
    CONSTRAINT fk_quiz_topic_tags_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id),
    CONSTRAINT fk_quiz_topic_tags_topic_tag FOREIGN KEY (topic_tag_id) REFERENCES topic_tags (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
