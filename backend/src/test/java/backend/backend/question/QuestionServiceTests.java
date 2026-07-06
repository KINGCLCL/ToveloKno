package backend.backend.question;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.classification.ClassificationService;
import backend.backend.common.BusinessException;
import backend.backend.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTests {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private OperationLogService operationLogService;

    @Mock
    private ClassificationService classificationService;

    private QuestionService questionService;
    private AuthenticatedUser currentUser;

    @BeforeEach
    void setUp() {
        questionService = new QuestionService(
                questionRepository,
                classificationService,
                operationLogService,
                new ObjectMapper());
        currentUser = new AuthenticatedUser(7L, "tester", List.of("USER"));
    }

    @Test
    void createQuestionStoresOwnerAndOptions() {
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(10L);
            question.prePersist();
            return question;
        });

        QuestionResponse response = questionService.createQuestion(validChoiceRequest(), currentUser);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCreatedBy()).isEqualTo(7L);
        assertThat(response.getOptions()).containsExactly("A. 原子性", "B. 一致性");
        assertThat(response.getStatus()).isEqualTo(QuestionStatus.PUBLISHED);
        verify(operationLogService).record(7L, "QUESTION_CREATE", "新增题目：数据库事务具有什么特性？");
    }

    @Test
    void choiceQuestionRequiresAtLeastTwoOptions() {
        QuestionSaveRequest request = validChoiceRequest();
        request.setOptions(List.of("只有一个选项"));

        assertThatThrownBy(() -> questionService.createQuestion(request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("选择题至少需要两个选项");
    }

    @Test
    void moveToRecycleBinUsesSoftDelete() {
        Question question = existingQuestion();
        when(questionRepository.findByIdAndCreatedBy(15L, 7L)).thenReturn(Optional.of(question));
        when(questionRepository.save(question)).thenReturn(question);

        questionService.moveToRecycleBin(15L, currentUser);

        assertThat(question.getDeleted()).isTrue();
        assertThat(question.getDeletedAt()).isNotNull();
        verify(questionRepository).save(question);
    }

    @Test
    void batchDeleteRejectsQuestionOwnedByAnotherUser() {
        QuestionBatchRequest request = new QuestionBatchRequest();
        request.setIds(List.of(1L, 2L));
        when(questionRepository.findAllByIdInAndCreatedBy(any(), eq(7L)))
                .thenReturn(List.of(existingQuestion()));

        assertThatThrownBy(() -> questionService.batchMoveToRecycleBin(request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("部分题目不存在或无权操作");
    }

    @Test
    void statsAreScopedToCurrentUser() {
        when(questionRepository.countByCreatedByAndDeletedFalse(7L)).thenReturn(12L);
        when(questionRepository.countByCreatedByAndDeletedFalseAndStatus(7L, QuestionStatus.PUBLISHED))
                .thenReturn(8L);
        when(questionRepository.countByCreatedByAndDeletedFalseAndStatus(7L, QuestionStatus.DRAFT))
                .thenReturn(4L);
        when(questionRepository.countByCreatedByAndDeletedTrue(7L)).thenReturn(3L);

        QuestionStatsResponse stats = questionService.getStats(currentUser);

        assertThat(stats.getTotal()).isEqualTo(12);
        assertThat(stats.getPublished()).isEqualTo(8);
        assertThat(stats.getDraft()).isEqualTo(4);
        assertThat(stats.getRecycleBin()).isEqualTo(3);
    }

    private QuestionSaveRequest validChoiceRequest() {
        QuestionSaveRequest request = new QuestionSaveRequest();
        request.setContent("数据库事务具有什么特性？");
        request.setQuestionType(QuestionType.SINGLE_CHOICE);
        request.setOptions(List.of("A. 原子性", "B. 一致性"));
        request.setCorrectAnswer("A");
        request.setAnalysis("事务具有 ACID 特性。");
        request.setDifficulty(3);
        request.setSubject("数据库");
        request.setKnowledgePoint("事务管理");
        request.setStatus(QuestionStatus.PUBLISHED);
        return request;
    }

    private Question existingQuestion() {
        Question question = new Question();
        question.setId(15L);
        question.setContent("测试题目");
        question.setQuestionType(QuestionType.TRUE_FALSE);
        question.setCorrectAnswer("正确");
        question.setDifficulty(2);
        question.setStatus(QuestionStatus.DRAFT);
        question.setCreatedBy(7L);
        question.setDeleted(false);
        return question;
    }
}
