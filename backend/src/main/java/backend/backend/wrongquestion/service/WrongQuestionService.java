package backend.backend.wrongquestion.service;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.service.OperationLogService;
import backend.backend.wrongquestion.dto.WrongQuestionResponse;
import backend.backend.wrongquestion.entity.WrongQuestion;
import backend.backend.wrongquestion.repository.WrongQuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.List;

/**
 * 错题本业务层。
 */
@Service
public class WrongQuestionService {

    private final WrongQuestionRepository wrongQuestionRepository;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public WrongQuestionService(
            WrongQuestionRepository wrongQuestionRepository,
            OperationLogService operationLogService,
            ObjectMapper objectMapper) {
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询当前用户的错题列表。
     *
     * mastered 为空：查全部
     * mastered=false：只查未掌握
     * mastered=true：只查已掌握
     */
    public List<WrongQuestionResponse> listWrongQuestions(AuthenticatedUser currentUser, Boolean mastered) {
        Integer masteredFlag = mastered == null ? null : (mastered ? 1 : 0);

        return wrongQuestionRepository.findWrongQuestionList(currentUser.getId(), masteredFlag)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 标记某道错题为已掌握。
     */
    @Transactional
    public void markAsMastered(Long wrongQuestionId, AuthenticatedUser currentUser) {
        WrongQuestion wrongQuestion = findOwnWrongQuestion(wrongQuestionId, currentUser.getId());

        wrongQuestion.setMastered(1);
        wrongQuestion.setLastReviewedAt(LocalDateTime.now());

        wrongQuestionRepository.save(wrongQuestion);

        operationLogService.record(
                currentUser.getId(),
                "WRONG_QUESTION_MASTERED",
                "标记掌握错题：" + wrongQuestionId
        );
    }

    /**
     * 将错题移出错题本。
     */
    @Transactional
    public void removeWrongQuestion(Long wrongQuestionId, AuthenticatedUser currentUser) {
        WrongQuestion wrongQuestion = findOwnWrongQuestion(wrongQuestionId, currentUser.getId());

        wrongQuestionRepository.delete(wrongQuestion);

        operationLogService.record(
                currentUser.getId(),
                "WRONG_QUESTION_REMOVE",
                "移出错题本：" + wrongQuestionId
        );
    }

    private WrongQuestion findOwnWrongQuestion(Long wrongQuestionId, Long userId) {
        return wrongQuestionRepository.findByIdAndUserId(wrongQuestionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("错题不存在或无权操作"));
    }

    private WrongQuestionResponse toResponse(WrongQuestionRepository.WrongQuestionView view) {
        return new WrongQuestionResponse(
                view.getId(),
                view.getQuestionId(),
                view.getContent(),
                view.getQuestionType(),
                readOptions(view.getOptionsJson()),
                view.getCorrectAnswer(),
                view.getAnalysis(),
                view.getDifficulty(),
                view.getSourceType(),
                view.getSourceResourceId(),
                view.getSourceResourceName(),
                view.getSourcePage(),
                view.getSourceExcerpt(),
                view.getWrongCount(),
                view.getMastered() != null && view.getMastered() == 1,
                view.getLastWrongAt(),
                view.getLastReviewedAt()
        );
    }

    private List<String> readOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
