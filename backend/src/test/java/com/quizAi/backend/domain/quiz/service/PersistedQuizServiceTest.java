package com.quizAi.backend.domain.quiz.service;

import com.quizAi.backend.domain.gemini.service.GeminiService;
import com.quizAi.backend.domain.member.entity.User;
import com.quizAi.backend.domain.member.entity.UserStatus;
import com.quizAi.backend.domain.member.service.MemberService;
import com.quizAi.backend.domain.quiz.dto.QuizDetailDto;
import com.quizAi.backend.domain.quiz.dto.QuizResultDto;
import com.quizAi.backend.domain.quiz.dto.TopicTagDto;
import com.quizAi.backend.domain.quiz.entity.Quiz;
import com.quizAi.backend.domain.quiz.entity.QuizCreatedVia;
import com.quizAi.backend.domain.quiz.entity.QuizQuestion;
import com.quizAi.backend.domain.quiz.entity.QuizVisibility;
import com.quizAi.backend.domain.quiz.entity.SourceType;
import com.quizAi.backend.domain.quiz.entity.TopicTag;
import com.quizAi.backend.global.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import(PersistedQuizService.class)
class PersistedQuizServiceTest {

    private static final List<String> TAG_SLUGS = List.of("backend", "database", "cs");
    private static final List<String> OPTIONS = List.of("보기 A", "보기 B", "보기 C", "보기 D");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PersistedQuizService persistedQuizService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private SourceMetadataResolver sourceMetadataResolver;

    @MockitoBean
    private GeminiService geminiService;

    @ParameterizedTest(name = "태그 {0}개인 공개 퀴즈도 문제 5개만 반환한다")
    @ValueSource(ints = {0, 1, 2, 3})
    void detailReturnsEachQuestionOnceRegardlessOfTagCount(int tagCount) {
        // 실제 저장과 조회를 거쳐 문제 중복, 순서, 내용, 태그 및 목록의 문제 수를 검증합니다.
        Quiz quiz = persistQuiz(tagCount, QuizVisibility.PUBLIC);

        QuizDetailDto detail = persistedQuizService.getQuizDetail(null, quiz.getId());

        assertQuizDetail(detail, tagCount);

        entityManager.clear();
        assertThat(persistedQuizService.getBoardQuizzes(null, null, 0, 12).getContent())
                .singleElement()
                .satisfies(item -> assertThat(item.getQuestionCount()).isEqualTo(5));

        entityManager.clear();
        assertThat(persistedQuizService.getMyQuizzes(quiz.getOwnerUser().getId(), null, null, 0, 12).getContent())
                .singleElement()
                .satisfies(item -> assertThat(item.getQuestionCount()).isEqualTo(5));
    }

    @Test
    @DisplayName("작성자는 태그가 여러 개인 비공개 퀴즈도 중복 없이 조회할 수 있다")
    void ownerCanReadPrivateQuizWithoutDuplicateQuestions() {
        // 비공개 퀴즈의 작성자 조회 권한과 문제 중복 방지를 함께 확인합니다.
        Quiz quiz = persistQuiz(2, QuizVisibility.PRIVATE);

        QuizDetailDto detail = persistedQuizService.getQuizDetail(quiz.getOwnerUser().getId(), quiz.getId());

        assertQuizDetail(detail, 2);
    }

    @ParameterizedTest(name = "요청자 {0}에게 비공개 퀴즈를 공개하지 않는다")
    @NullSource
    @ValueSource(longs = {-1})
    void privateQuizIsHiddenFromGuestsAndOtherUsers(Long requesterId) {
        // 조회 설정이 변경되어도 게스트와 다른 회원의 비공개 퀴즈 접근을 차단합니다.
        Quiz quiz = persistQuiz(2, QuizVisibility.PRIVATE);

        assertThatThrownBy(() -> persistedQuizService.getQuizDetail(requesterId, quiz.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Quiz persistQuiz(int tagCount, QuizVisibility visibility) {
        // AI 호출 없이 테스트 DB에 퀴즈를 저장하고 영속성 컨텍스트를 비워 실제 재조회를 보장합니다.
        User owner = entityManager.persist(User.builder()
                .email("quiz-test@example.com")
                .nickname("테스트 작성자")
                .status(UserStatus.ACTIVE)
                .build());

        Quiz quiz = Quiz.builder()
                .ownerUser(owner)
                .title("중복 조회 검증 퀴즈")
                .sourceUrl("https://example.com/article")
                .sourceType(SourceType.BLOG)
                .sourceHost("example.com")
                .visibility(visibility)
                .createdVia(QuizCreatedVia.MEMBER_GENERATED)
                .publishedAt(LocalDateTime.now())
                .build();

        for (int index = 0; index < tagCount; index++) {
            TopicTag tag = entityManager.persist(TopicTag.builder()
                    .slug(TAG_SLUGS.get(index))
                    .displayName("테스트 태그 " + index)
                    .build());
            quiz.addTopicTags(List.of(tag));
        }

        for (int sortOrder = 5; sortOrder >= 1; sortOrder--) {
            quiz.addQuestion(QuizQuestion.builder()
                    .quiz(quiz)
                    .sortOrder(sortOrder)
                    .question("문제 " + sortOrder)
                    .options(OPTIONS)
                    .answer("보기 B")
                    .explanation("해설 " + sortOrder)
                    .codeSnippet("코드 " + sortOrder)
                    .build());
        }

        entityManager.persistAndFlush(quiz);
        entityManager.clear();
        return quiz;
    }

    private void assertQuizDetail(QuizDetailDto detail, int tagCount) {
        // 실제 저장된 문제의 수와 순서, 선택지·정답·해설·코드 및 태그가 상세 응답에 보존되는지 확인합니다.
        assertThat(detail.getQuiz().getQuestionCount()).isEqualTo(5);
        assertThat(detail.getQuizResult().getQuestions())
                .extracting(QuizResultDto.QuestionDto::getId)
                .containsExactly(1, 2, 3, 4, 5);
        assertThat(detail.getQuizResult().getQuestions()).allSatisfy(question -> {
            assertThat(question.getQuestion()).isEqualTo("문제 " + question.getId());
            assertThat(question.getOptions()).containsExactlyElementsOf(OPTIONS);
            assertThat(question.getAnswer()).isEqualTo("보기 B");
            assertThat(question.getExplanation()).isEqualTo("해설 " + question.getId());
            assertThat(question.getCodeSnippet()).isEqualTo("코드 " + question.getId());
        });
        assertThat(detail.getTopicTags()).extracting(TopicTagDto::getSlug)
                .containsExactlyInAnyOrderElementsOf(TAG_SLUGS.subList(0, tagCount));
        assertThat(detail.getQuiz().getTopicTags()).extracting(TopicTagDto::getSlug)
                .containsExactlyInAnyOrderElementsOf(TAG_SLUGS.subList(0, tagCount));
    }
}
