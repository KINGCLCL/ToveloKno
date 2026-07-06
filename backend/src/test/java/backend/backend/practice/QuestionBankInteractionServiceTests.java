package backend.backend.practice;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.classification.ClassificationService;
import backend.backend.question.Question;
import backend.backend.question.QuestionRepository;
import backend.backend.question.QuestionStatus;
import backend.backend.question.QuestionType;
import backend.backend.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionBankInteractionServiceTests {

    @Mock private AnswerRecordRepository answerRecordRepository;
    @Mock private WrongQuestionRepository wrongQuestionRepository;
    @Mock private QuestionBankSettingRepository settingRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private ClassificationService classificationService;
    @Mock private OperationLogService operationLogService;

    private QuestionBankInteractionService service;
    private AuthenticatedUser user;

    @BeforeEach
    void setUp() {
        service = new QuestionBankInteractionService(
                answerRecordRepository,
                wrongQuestionRepository,
                settingRepository,
                questionRepository,
                classificationService,
                operationLogService,
                new ObjectMapper());
        user = new AuthenticatedUser(7L, "tester", List.of("USER"));
    }

    @Test
    void generatePracticeOnlyReturnsPracticeFields() {
        Question question = publishedQuestion();
        when(questionRepository.findAllByCreatedByAndDeletedFalseAndStatus(7L, QuestionStatus.PUBLISHED))
                .thenReturn(List.of(question));

        List<QuestionBankDtos.PracticeQuestion> generated = service.generatePractice(
                new QuestionBankDtos.PracticeRequest("free", 5, null, null), user);

        assertThat(generated).hasSize(1);
        assertThat(generated.get(0).content()).isEqualTo("数据库事务具有什么特性？");
        assertThat(generated.get(0).options()).containsExactly("A. 原子性", "B. 随机性");
    }

    @Test
    void wrongAnswerCreatesAnswerRecordAndWrongQuestion() {
        Question question = publishedQuestion();
        when(questionRepository.findByIdAndCreatedBy(10L, 7L)).thenReturn(Optional.of(question));
        when(wrongQuestionRepository.findByUserIdAndQuestionId(7L, 10L)).thenReturn(Optional.empty());
        when(wrongQuestionRepository.save(any(WrongQuestion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuestionBankDtos.AnswerResult result = service.submitAnswer(
                new QuestionBankDtos.AnswerRequest(10L, "B", "free"), user);

        assertThat(result.correct()).isFalse();
        assertThat(result.correctAnswer()).isEqualTo("A");
        assertThat(result.wrongCount()).isEqualTo(1);
        verify(answerRecordRepository).save(any(AnswerRecord.class));
        ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
        verify(wrongQuestionRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getQuestionId()).isEqualTo(10L);
    }

    @Test
    void correctWrongBookReviewMarksQuestionMastered() {
        Question question = publishedQuestion();
        WrongQuestion wrongQuestion = new WrongQuestion();
        wrongQuestion.setUserId(7L);
        wrongQuestion.setQuestionId(10L);
        wrongQuestion.setWrongCount(2);
        wrongQuestion.setMastered(false);
        when(questionRepository.findByIdAndCreatedBy(10L, 7L)).thenReturn(Optional.of(question));
        when(wrongQuestionRepository.findByUserIdAndQuestionId(7L, 10L))
                .thenReturn(Optional.of(wrongQuestion));
        when(wrongQuestionRepository.save(any(WrongQuestion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuestionBankDtos.AnswerResult result = service.submitAnswer(
                new QuestionBankDtos.AnswerRequest(10L, "a", "wrong"), user);

        assertThat(result.correct()).isTrue();
        assertThat(result.mastered()).isTrue();
        assertThat(wrongQuestion.getLastReviewedAt()).isNotNull();
    }

    private Question publishedQuestion() {
        Question question = new Question();
        question.setId(10L);
        question.setContent("数据库事务具有什么特性？");
        question.setQuestionType(QuestionType.SINGLE_CHOICE);
        question.setOptionsJson("[\"A. 原子性\",\"B. 随机性\"]");
        question.setCorrectAnswer("A");
        question.setAnalysis("事务具有 ACID 特性。");
        question.setDifficulty(3);
        question.setSubject("数据库");
        question.setKnowledgePoint("事务管理");
        question.setStatus(QuestionStatus.PUBLISHED);
        question.setCreatedBy(7L);
        question.setDeleted(false);
        return question;
    }
}
