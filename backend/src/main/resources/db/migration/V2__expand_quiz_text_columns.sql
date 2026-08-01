ALTER TABLE quizzes
    MODIFY COLUMN source_url TEXT NOT NULL;

ALTER TABLE quiz_questions
    MODIFY COLUMN question TEXT NOT NULL,
    MODIFY COLUMN explanation TEXT NOT NULL,
    MODIFY COLUMN code_snippet TEXT NULL;
