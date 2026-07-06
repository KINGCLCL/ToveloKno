package backend.backend.question;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.classification.Category;
import backend.backend.classification.ClassificationService;
import backend.backend.common.BusinessException;
import backend.backend.common.PageResponse;
import backend.backend.service.OperationLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 题目管理业务服务。
 *
 * 所有查询和写操作都限定 createdBy，确保用户之间的数据隔离。
 */
@Service
public class QuestionService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "updatedAt", "difficulty", "status", "questionType");

    private final QuestionRepository questionRepository;
    private final ClassificationService classificationService;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public QuestionService(
            QuestionRepository questionRepository,
            ClassificationService classificationService,
            OperationLogService operationLogService,
            ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.classificationService = classificationService;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页筛选当前用户的题目。
     */
    @Transactional(readOnly = true)
    public PageResponse<QuestionResponse> listQuestions(
            AuthenticatedUser currentUser,
            int page,
            int size,
            String keyword,
            String subject,
            String knowledgePoint,
            Long categoryId,
            QuestionType questionType,
            Integer difficulty,
            QuestionStatus status,
            boolean deleted,
            String sort,
            String direction) {
        String sortField = ALLOWED_SORT_FIELDS.contains(sort) ? sort : "updatedAt";
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Set<Long> categoryIds = categoryId == null
                ? null
                : classificationService.getOwnedCategoryAndDescendantIds(categoryId, currentUser.getId());

        Specification<Question> specification = ownerIs(currentUser.getId())
                .and(deletedIs(deleted))
                .and(keywordContains(keyword))
                .and(fieldEquals("subject", normalizeText(subject)))
                .and(fieldEquals("knowledgePoint", normalizeText(knowledgePoint)))
                .and(fieldIn("categoryId", categoryIds))
                .and(fieldEquals("questionType", questionType))
                .and(fieldEquals("difficulty", difficulty))
                .and(fieldEquals("status", status));

        Page<QuestionResponse> result = questionRepository
                .findAll(specification, PageRequest.of(page - 1, size, Sort.by(sortDirection, sortField)))
                .map(this::toResponse);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(Long questionId, AuthenticatedUser currentUser) {
        return toResponse(findOwnedQuestion(questionId, currentUser.getId()));
    }

    @Transactional
    public QuestionResponse createQuestion(QuestionSaveRequest request, AuthenticatedUser currentUser) {
        validateQuestionRules(request);
        Category category = classificationService.validateOwnedCategory(request.getCategoryId(), currentUser.getId());

        Question question = new Question();
        question.setCreatedBy(currentUser.getId());
        question.setDeleted(false);
        applyRequest(question, request);
        applyCategorySubject(question, category);

        Question saved = questionRepository.save(question);
        operationLogService.record(
                currentUser.getId(),
                "QUESTION_CREATE",
                "新增题目：" + summarize(saved.getContent()));
        return toResponse(saved);
    }

    @Transactional
    public QuestionResponse updateQuestion(
            Long questionId,
            QuestionSaveRequest request,
            AuthenticatedUser currentUser) {
        validateQuestionRules(request);
        Category category = classificationService.validateOwnedCategory(request.getCategoryId(), currentUser.getId());
        Question question = findOwnedQuestion(questionId, currentUser.getId());
        ensureNotDeleted(question);

        applyRequest(question, request);
        applyCategorySubject(question, category);
        Question saved = questionRepository.save(question);
        operationLogService.record(
                currentUser.getId(),
                "QUESTION_UPDATE",
                "修改题目：" + summarize(saved.getContent()));
        return toResponse(saved);
    }

    /**
     * 单题软删除：移入回收站。
     */
    @Transactional
    public void moveToRecycleBin(Long questionId, AuthenticatedUser currentUser) {
        Question question = findOwnedQuestion(questionId, currentUser.getId());
        markDeleted(question);
        questionRepository.save(question);
        operationLogService.record(currentUser.getId(), "QUESTION_DELETE", "题目移入回收站：" + questionId);
    }

    /**
     * 批量软删除，单次最多100题。
     */
    @Transactional
    public int batchMoveToRecycleBin(QuestionBatchRequest request, AuthenticatedUser currentUser) {
        List<Question> questions = findOwnedQuestions(request.getIds(), currentUser.getId());
        questions.forEach(this::markDeleted);
        questionRepository.saveAll(questions);
        operationLogService.record(
                currentUser.getId(),
                "QUESTION_BATCH_DELETE",
                "批量移入回收站：" + questions.size() + "题");
        return questions.size();
    }

    @Transactional
    public QuestionResponse restoreQuestion(Long questionId, AuthenticatedUser currentUser) {
        Question question = findOwnedQuestion(questionId, currentUser.getId());
        if (!Boolean.TRUE.equals(question.getDeleted())) {
            throw new BusinessException("题目不在回收站中");
        }
        question.setDeleted(false);
        question.setDeletedAt(null);
        Question saved = questionRepository.save(question);
        operationLogService.record(currentUser.getId(), "QUESTION_RESTORE", "恢复题目：" + questionId);
        return toResponse(saved);
    }

    /**
     * 永久删除仅允许操作回收站中的题目。
     */
    @Transactional
    public void permanentlyDelete(Long questionId, AuthenticatedUser currentUser) {
        Question question = findOwnedQuestion(questionId, currentUser.getId());
        if (!Boolean.TRUE.equals(question.getDeleted())) {
            throw new BusinessException("请先将题目移入回收站");
        }
        questionRepository.delete(question);
        operationLogService.record(currentUser.getId(), "QUESTION_PERMANENT_DELETE", "永久删除题目：" + questionId);
    }

    @Transactional
    public int batchUpdateStatus(QuestionBatchStatusRequest request, AuthenticatedUser currentUser) {
        List<Question> questions = findOwnedQuestions(request.getIds(), currentUser.getId());
        questions.forEach(question -> {
            ensureNotDeleted(question);
            question.setStatus(request.getStatus());
        });
        questionRepository.saveAll(questions);
        operationLogService.record(
                currentUser.getId(),
                "QUESTION_BATCH_STATUS",
                "批量修改题目状态为" + request.getStatus() + "：" + questions.size() + "题");
        return questions.size();
    }

    @Transactional(readOnly = true)
    public QuestionStatsResponse getStats(AuthenticatedUser currentUser) {
        Long userId = currentUser.getId();
        return new QuestionStatsResponse(
                questionRepository.countByCreatedByAndDeletedFalse(userId),
                questionRepository.countByCreatedByAndDeletedFalseAndStatus(userId, QuestionStatus.PUBLISHED),
                questionRepository.countByCreatedByAndDeletedFalseAndStatus(userId, QuestionStatus.DRAFT),
                questionRepository.countByCreatedByAndDeletedTrue(userId));
    }

    private void applyRequest(Question question, QuestionSaveRequest request) {
        question.setContent(request.getContent().trim());
        question.setQuestionType(request.getQuestionType());
        question.setOptionsJson(writeOptions(normalizeOptions(request)));
        question.setCorrectAnswer(request.getCorrectAnswer().trim());
        question.setAnalysis(normalizeText(request.getAnalysis()));
        question.setDifficulty(request.getDifficulty());
        question.setSubject(normalizeText(request.getSubject()));
        question.setKnowledgePoint(normalizeText(request.getKnowledgePoint()));
        question.setStatus(request.getStatus());
        question.setCategoryId(request.getCategoryId());
    }

    private void applyCategorySubject(Question question, Category category) {
        if (category != null && question.getSubject() == null) {
            question.setSubject(category.getName());
        }
    }

    /**
     * 选择题至少需要两个选项；判断题未传选项时自动补全。
     */
    private void validateQuestionRules(QuestionSaveRequest request) {
        List<String> options = request.getOptions();
        if ((request.getQuestionType() == QuestionType.SINGLE_CHOICE
                || request.getQuestionType() == QuestionType.MULTIPLE_CHOICE)
                && (options == null || options.size() < 2)) {
            throw new BusinessException("选择题至少需要两个选项");
        }
    }

    private List<String> normalizeOptions(QuestionSaveRequest request) {
        if (request.getQuestionType() == QuestionType.TRUE_FALSE
                && (request.getOptions() == null || request.getOptions().isEmpty())) {
            return List.of("正确", "错误");
        }
        if (request.getOptions() == null || request.getOptions().isEmpty()) {
            return List.of();
        }
        return request.getOptions().stream().map(String::trim).toList();
    }

    private String writeOptions(List<String> options) {
        if (options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("题目选项序列化失败");
        }
    }

    private List<String> readOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getContent(),
                question.getQuestionType(),
                readOptions(question.getOptionsJson()),
                question.getCorrectAnswer(),
                question.getAnalysis(),
                question.getDifficulty(),
                question.getSubject(),
                question.getKnowledgePoint(),
                question.getStatus(),
                question.getCategoryId(),
                question.getCreatedBy(),
                Boolean.TRUE.equals(question.getDeleted()),
                question.getDeletedAt(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }

    private Question findOwnedQuestion(Long questionId, Long userId) {
        return questionRepository.findByIdAndCreatedBy(questionId, userId)
                .orElseThrow(() -> new BusinessException("题目不存在或无权访问"));
    }

    private List<Question> findOwnedQuestions(List<Long> requestedIds, Long userId) {
        Set<Long> ids = new LinkedHashSet<>(requestedIds);
        if (ids.contains(null)) {
            throw new BusinessException("题目ID不能为空");
        }
        List<Question> questions = questionRepository.findAllByIdInAndCreatedBy(ids, userId);
        if (questions.size() != ids.size()) {
            throw new BusinessException("部分题目不存在或无权操作");
        }
        return questions;
    }

    private void markDeleted(Question question) {
        if (!Boolean.TRUE.equals(question.getDeleted())) {
            question.setDeleted(true);
            question.setDeletedAt(LocalDateTime.now());
        }
    }

    private void ensureNotDeleted(Question question) {
        if (Boolean.TRUE.equals(question.getDeleted())) {
            throw new BusinessException("回收站中的题目不能执行此操作");
        }
    }

    private Specification<Question> ownerIs(Long userId) {
        return (root, query, builder) -> builder.equal(root.get("createdBy"), userId);
    }

    private Specification<Question> deletedIs(boolean deleted) {
        return (root, query, builder) -> builder.equal(root.get("deleted"), deleted);
    }

    private Specification<Question> keywordContains(String keyword) {
        String normalized = normalizeText(keyword);
        if (normalized == null) {
            return null;
        }
        String pattern = "%" + normalized.toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("content")), pattern),
                builder.like(builder.lower(root.get("subject")), pattern),
                builder.like(builder.lower(root.get("knowledgePoint")), pattern));
    }

    private <T> Specification<Question> fieldEquals(String field, T value) {
        if (value == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get(field), value);
    }

    private Specification<Question> fieldIn(String field, Set<?> values) {
        if (values == null) {
            return null;
        }
        return (root, query, builder) -> root.get(field).in(values);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String summarize(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 60 ? normalized : normalized.substring(0, 60) + "...";
    }
}
