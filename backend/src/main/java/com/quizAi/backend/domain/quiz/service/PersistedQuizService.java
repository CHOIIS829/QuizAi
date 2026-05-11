package com.quizAi.backend.domain.quiz.service;

import com.quizAi.backend.domain.member.entity.User;
import com.quizAi.backend.domain.member.service.MemberService;
import com.quizAi.backend.domain.gemini.service.GeminiService;
import com.quizAi.backend.domain.quiz.dto.*;
import com.quizAi.backend.domain.quiz.entity.*;
import com.quizAi.backend.domain.quiz.repository.QuizRepository;
import com.quizAi.backend.domain.quiz.repository.TopicTagRepository;
import com.quizAi.backend.global.exception.ResourceNotFoundException;
import com.quizAi.backend.global.response.PageResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PersistedQuizService {

    private final QuizRepository quizRepository;
    private final TopicTagRepository topicTagRepository;
    private final MemberService memberService;
    private final SourceMetadataResolver sourceMetadataResolver;
    private final GeminiService geminiService;

    public PersistedQuizService(
            QuizRepository quizRepository,
            TopicTagRepository topicTagRepository,
            MemberService memberService,
            SourceMetadataResolver sourceMetadataResolver,
            GeminiService geminiService
    ) {
        this.quizRepository = quizRepository;
        this.topicTagRepository = topicTagRepository;
        this.memberService = memberService;
        this.sourceMetadataResolver = sourceMetadataResolver;
        this.geminiService = geminiService;
    }

    @Transactional
    public Long persistQuiz(Long userId, String sourceUrl, QuizResultDto quizResultDto, QuizCreatedVia createdVia) {
        User user = memberService.getRequiredUser(userId);
        SourceMetadata sourceMetadata = sourceMetadataResolver.resolve(sourceUrl);
        List<TopicTag> topicTags = resolveTopicTags(quizResultDto);

        Quiz quiz = Quiz.builder()
                .ownerUser(user)
                .title(resolveTitle(quizResultDto))
                .sourceUrl(sourceUrl)
                .sourceType(sourceMetadata.sourceType())
                .sourceHost(sourceMetadata.sourceHost())
                .visibility(QuizVisibility.PUBLIC)
                .createdVia(createdVia)
                .publishedAt(LocalDateTime.now())
                .build();

        List<QuizResultDto.QuestionDto> sortedQuestions = new ArrayList<>(quizResultDto.getQuestions());
        sortedQuestions.sort(Comparator.comparingInt(QuizResultDto.QuestionDto::getId));

        for (int index = 0; index < sortedQuestions.size(); index++) {
            QuizResultDto.QuestionDto questionDto = sortedQuestions.get(index);
            quiz.addQuestion(QuizQuestion.builder()
                    .quiz(quiz)
                    .sortOrder(index + 1)
                    .question(questionDto.getQuestion())
                    .options(questionDto.getOptions())
                    .answer(questionDto.getAnswer())
                    .explanation(questionDto.getExplanation())
                    .codeSnippet(questionDto.getCodeSnippet())
                    .build());
        }

        quiz.addTopicTags(topicTags);

        return quizRepository.save(quiz).getId();
    }

    @Transactional(readOnly = true)
    public PageResponseDto<QuizListItemDto> getMyQuizzes(Long userId, SourceType sourceType, String tag, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Specification<Quiz> specification = QuizSpecifications.ownerIdEquals(userId);
        specification = applyFilters(specification, sourceType, tag);

        Page<QuizListItemDto> result = quizRepository.findAll(specification, pageable)
                .map(this::toListItemDto);

        return PageResponseDto.from(result);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<QuizListItemDto> getBoardQuizzes(SourceType sourceType, String tag, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Specification<Quiz> specification = QuizSpecifications.visibilityEquals(QuizVisibility.PUBLIC);
        specification = applyFilters(specification, sourceType, tag);

        Page<QuizListItemDto> result = quizRepository.findAll(specification, pageable)
                .map(this::toListItemDto);

        return PageResponseDto.from(result);
    }

    @Transactional(readOnly = true)
    public QuizDetailDto getQuizDetail(Long requesterUserId, Long quizId) {
        Quiz quiz = quizRepository.findDetailById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("퀴즈를 찾을 수 없습니다.", "QUIZ_NOT_FOUND"));

        if (quiz.getVisibility() != QuizVisibility.PUBLIC
                && (requesterUserId == null || !quiz.getOwnerUser().getId().equals(requesterUserId))) {
            throw new ResourceNotFoundException("퀴즈를 찾을 수 없습니다.", "QUIZ_NOT_FOUND");
        }

        QuizListItemDto summary = toListItemDto(quiz);
        return QuizDetailDto.builder()
                .quiz(summary)
                .topicTags(summary.getTopicTags())
                .quizResult(QuizResultDto.builder()
                        .title(quiz.getTitle())
                        .questions(quiz.getQuestions().stream()
                                .map(item -> QuizResultDto.QuestionDto.builder()
                                        .id(item.getSortOrder())
                                        .question(item.getQuestion())
                                        .options(item.getOptions())
                                        .answer(item.getAnswer())
                                        .explanation(item.getExplanation())
                                        .codeSnippet(item.getCodeSnippet())
                                        .build())
                                .toList())
                        .build())
                .build();
    }

    private Specification<Quiz> applyFilters(Specification<Quiz> specification, SourceType sourceType, String tag) {
        if (sourceType != null) {
            specification = specification.and(QuizSpecifications.sourceTypeEquals(sourceType));
        }
        if (StringUtils.hasText(tag)) {
            specification = specification.and(QuizSpecifications.hasTag(tag));
        }
        return specification;
    }

    private QuizListItemDto toListItemDto(Quiz quiz) {
        return QuizListItemDto.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .authorNickname(quiz.getOwnerUser().getNickname())
                .sourceUrl(quiz.getSourceUrl())
                .sourceType(quiz.getSourceType())
                .sourceHost(quiz.getSourceHost())
                .publishedAt(quiz.getPublishedAt())
                .questionCount(quiz.getQuestions().size())
                .topicTags(quiz.getTopicTags().stream()
                        .map(item -> TopicTagDto.builder()
                                .slug(item.getSlug())
                                .displayName(item.getDisplayName())
                                .build())
                        .toList())
                .build();
    }

    private List<TopicTag> resolveTopicTags(QuizResultDto quizResultDto) {
        List<String> tags = geminiService.selectTopicTags(quizResultDto);
        List<TopicTag> topicTags = topicTagRepository.findBySlugIn(tags);
        return topicTags.isEmpty()
                ? topicTagRepository.findBySlugIn(TopicTagCatalog.inferTags(quizResultDto))
                : topicTags;
    }

    private String resolveTitle(QuizResultDto quizResultDto) {
        return StringUtils.hasText(quizResultDto.getTitle()) ? quizResultDto.getTitle().trim() : "생성된 퀴즈";
    }
}
