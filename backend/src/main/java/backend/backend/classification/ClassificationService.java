package backend.backend.classification;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.common.BusinessException;
import backend.backend.question.Question;
import backend.backend.question.QuestionRepository;
import backend.backend.question.QuestionStatus;
import backend.backend.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ClassificationService {

    private static final Map<Integer, String> DIFFICULTY_LABELS = Map.of(
            1, "入门",
            2, "简单",
            3, "中等",
            4, "困难",
            5, "挑战");

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final QuestionRepository questionRepository;
    private final OperationLogService operationLogService;

    public ClassificationService(
            CategoryRepository categoryRepository,
            TagRepository tagRepository,
            QuestionRepository questionRepository,
            OperationLogService operationLogService) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.questionRepository = questionRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public ClassificationOverviewResponse getOverview(AuthenticatedUser currentUser) {
        Long userId = currentUser.getId();
        List<Category> categories = categoryRepository.findAllByCreatedByOrderBySortOrderAscNameAsc(userId);
        List<Tag> tags = tagRepository.findAllByCreatedByOrderByNameAsc(userId);
        Map<Long, Long> directQuestionCounts = categoryQuestionCounts(userId);
        Map<Long, Category> categoriesById = new LinkedHashMap<>();
        categories.forEach(category -> categoriesById.put(category.getId(), category));

        List<ClassificationOverviewResponse.CategoryItem> categoryItems = categories.stream()
                .map(category -> toCategoryItem(category, categoriesById, directQuestionCounts))
                .toList();
        List<ClassificationOverviewResponse.CategoryNode> categoryTree =
                buildCategoryTree(categories, categoriesById, directQuestionCounts);
        List<ClassificationOverviewResponse.KnowledgePointItem> knowledgePoints = knowledgePointItems(userId);
        List<ClassificationOverviewResponse.DifficultyItem> difficulties = difficultyItems(userId);
        List<ClassificationOverviewResponse.TagItem> tagItems = tags.stream().map(this::toTagItem).toList();

        return new ClassificationOverviewResponse(
                categoryItems,
                categoryTree,
                knowledgePoints,
                tagItems,
                difficulties,
                new ClassificationOverviewResponse.Summary(
                        categories.size(),
                        knowledgePoints.size(),
                        tags.size(),
                        questionRepository.countByCreatedByAndDeletedFalseAndCategoryIdIsNotNull(userId),
                        questionRepository.countByCreatedByAndDeletedFalseAndCategoryIdIsNull(userId)));
    }

    @Transactional
    public ClassificationOverviewResponse.CategoryItem createCategory(
            CategorySaveRequest request,
            AuthenticatedUser currentUser) {
        Long userId = currentUser.getId();
        String name = normalizeRequired(request.getName());
        Category parent = resolveParent(request.getParentId(), userId);
        ensureCategoryNameAvailable(name, parent == null ? null : parent.getId(), userId, null);

        Category category = new Category();
        category.setCreatedBy(userId);
        applyCategoryRequest(category, request, name, parent);
        Category saved = categoryRepository.save(category);
        operationLogService.record(userId, "CATEGORY_CREATE", "新增分类：" + saved.getName());
        return toCategoryItem(saved, categoryMap(userId), categoryQuestionCounts(userId));
    }

    @Transactional
    public ClassificationOverviewResponse.CategoryItem updateCategory(
            Long categoryId,
            CategorySaveRequest request,
            AuthenticatedUser currentUser) {
        Long userId = currentUser.getId();
        Category category = findOwnedCategory(categoryId, userId);
        String oldName = category.getName();
        String name = normalizeRequired(request.getName());
        Category parent = resolveParent(request.getParentId(), userId);
        ensureNoCategoryCycle(categoryId, parent, userId);
        ensureCategoryNameAvailable(name, parent == null ? null : parent.getId(), userId, categoryId);

        applyCategoryRequest(category, request, name, parent);
        Category saved = categoryRepository.save(category);
        if (!oldName.equals(name)) {
            List<Question> linkedQuestions =
                    questionRepository.findAllByCreatedByAndDeletedFalseAndCategoryId(userId, categoryId);
            linkedQuestions.forEach(question -> question.setSubject(name));
            questionRepository.saveAll(linkedQuestions);
        }
        operationLogService.record(userId, "CATEGORY_UPDATE", "修改分类：" + oldName + " → " + name);
        return toCategoryItem(saved, categoryMap(userId), categoryQuestionCounts(userId));
    }

    @Transactional
    public void deleteCategory(Long categoryId, AuthenticatedUser currentUser) {
        Long userId = currentUser.getId();
        Category category = findOwnedCategory(categoryId, userId);
        if (categoryRepository.existsByCreatedByAndParentId(userId, categoryId)) {
            throw new BusinessException("该分类下还有子分类，请先移动或删除子分类");
        }
        if (!questionRepository.findAllByCreatedByAndDeletedFalseAndCategoryId(userId, categoryId).isEmpty()) {
            throw new BusinessException("该分类仍关联题目，请先调整题目分类");
        }
        categoryRepository.delete(category);
        operationLogService.record(userId, "CATEGORY_DELETE", "删除分类：" + category.getName());
    }

    @Transactional
    public ClassificationOverviewResponse.TagItem createTag(
            TagSaveRequest request,
            AuthenticatedUser currentUser) {
        Long userId = currentUser.getId();
        String name = normalizeRequired(request.getName());
        if (tagRepository.existsByCreatedByAndNameIgnoreCase(userId, name)) {
            throw new BusinessException("同名标签已存在");
        }
        Tag tag = new Tag();
        tag.setName(name);
        tag.setDescription(normalizeText(request.getDescription()));
        tag.setCreatedBy(userId);
        Tag saved = tagRepository.save(tag);
        operationLogService.record(userId, "TAG_CREATE", "新增标签：" + saved.getName());
        return toTagItem(saved);
    }

    @Transactional
    public ClassificationOverviewResponse.TagItem updateTag(
            Long tagId,
            TagSaveRequest request,
            AuthenticatedUser currentUser) {
        Long userId = currentUser.getId();
        Tag tag = findOwnedTag(tagId, userId);
        String name = normalizeRequired(request.getName());
        boolean duplicate = tagRepository.findAllByCreatedByOrderByNameAsc(userId).stream()
                .anyMatch(candidate -> !candidate.getId().equals(tagId)
                        && candidate.getName().equalsIgnoreCase(name));
        if (duplicate) {
            throw new BusinessException("同名标签已存在");
        }
        tag.setName(name);
        tag.setDescription(normalizeText(request.getDescription()));
        Tag saved = tagRepository.save(tag);
        operationLogService.record(userId, "TAG_UPDATE", "修改标签：" + saved.getName());
        return toTagItem(saved);
    }

    @Transactional
    public void deleteTag(Long tagId, AuthenticatedUser currentUser) {
        Tag tag = findOwnedTag(tagId, currentUser.getId());
        tagRepository.delete(tag);
        operationLogService.record(currentUser.getId(), "TAG_DELETE", "删除标签：" + tag.getName());
    }

    @Transactional
    public int renameKnowledgePoint(
            KnowledgePointRenameRequest request,
            AuthenticatedUser currentUser) {
        String oldName = normalizeRequired(request.getOldName());
        String newName = normalizeRequired(request.getNewName());
        String subject = normalizeText(request.getSubject());
        List<Question> questions =
                questionRepository.findByKnowledgePoint(currentUser.getId(), oldName, subject);
        if (questions.isEmpty()) {
            throw new BusinessException("知识点不存在或没有关联题目");
        }
        questions.forEach(question -> question.setKnowledgePoint(newName));
        questionRepository.saveAll(questions);
        operationLogService.record(
                currentUser.getId(),
                "KNOWLEDGE_POINT_RENAME",
                "知识点重命名：" + oldName + " → " + newName + "（" + questions.size() + "题）");
        return questions.size();
    }

    @Transactional
    public int clearKnowledgePoint(String name, String subject, AuthenticatedUser currentUser) {
        String normalizedName = normalizeRequired(name);
        String normalizedSubject = normalizeText(subject);
        List<Question> questions =
                questionRepository.findByKnowledgePoint(currentUser.getId(), normalizedName, normalizedSubject);
        if (questions.isEmpty()) {
            throw new BusinessException("知识点不存在或没有关联题目");
        }
        questions.forEach(question -> question.setKnowledgePoint(null));
        questionRepository.saveAll(questions);
        operationLogService.record(
                currentUser.getId(),
                "KNOWLEDGE_POINT_CLEAR",
                "移除知识点：" + normalizedName + "（" + questions.size() + "题）");
        return questions.size();
    }

    @Transactional(readOnly = true)
    public Category validateOwnedCategory(Long categoryId, Long userId) {
        if (categoryId == null) {
            return null;
        }
        Category category = findOwnedCategory(categoryId, userId);
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new BusinessException("所选分类已停用");
        }
        return category;
    }

    @Transactional(readOnly = true)
    public Set<Long> getOwnedCategoryAndDescendantIds(Long categoryId, Long userId) {
        findOwnedCategory(categoryId, userId);
        List<Category> categories = categoryRepository.findAllByCreatedByOrderBySortOrderAscNameAsc(userId);
        Set<Long> result = new HashSet<>();
        result.add(categoryId);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Category category : categories) {
                if (result.contains(category.getParentId()) && result.add(category.getId())) {
                    changed = true;
                }
            }
        }
        return result;
    }

    private List<ClassificationOverviewResponse.KnowledgePointItem> knowledgePointItems(Long userId) {
        return questionRepository.aggregateKnowledgePoints(userId, QuestionStatus.PUBLISHED).stream()
                .map(row -> new ClassificationOverviewResponse.KnowledgePointItem(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue(),
                        (LocalDateTime) row[4]))
                .toList();
    }

    private List<ClassificationOverviewResponse.DifficultyItem> difficultyItems(Long userId) {
        Map<Integer, Long> counts = new HashMap<>();
        questionRepository.aggregateDifficulties(userId).forEach(row ->
                counts.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        List<ClassificationOverviewResponse.DifficultyItem> result = new ArrayList<>();
        for (int level = 1; level <= 5; level++) {
            long count = counts.getOrDefault(level, 0L);
            double percentage = total == 0 ? 0 : Math.round(count * 1000.0 / total) / 10.0;
            result.add(new ClassificationOverviewResponse.DifficultyItem(
                    level,
                    DIFFICULTY_LABELS.get(level),
                    count,
                    percentage));
        }
        return result;
    }

    private List<ClassificationOverviewResponse.CategoryNode> buildCategoryTree(
            List<Category> categories,
            Map<Long, Category> categoriesById,
            Map<Long, Long> directCounts) {
        Map<Long, List<Category>> childrenByParent = new HashMap<>();
        categories.forEach(category ->
                childrenByParent.computeIfAbsent(category.getParentId(), ignored -> new ArrayList<>()).add(category));

        List<Category> roots = categories.stream()
                .filter(category -> category.getParentId() == null
                        || !categoriesById.containsKey(category.getParentId()))
                .toList();
        return roots.stream()
                .map(category -> toCategoryNode(category, childrenByParent, directCounts, new HashSet<>()))
                .toList();
    }

    private ClassificationOverviewResponse.CategoryNode toCategoryNode(
            Category category,
            Map<Long, List<Category>> childrenByParent,
            Map<Long, Long> directCounts,
            Set<Long> path) {
        if (!path.add(category.getId())) {
            return new ClassificationOverviewResponse.CategoryNode(
                    category.getId(), category.getName(), Boolean.TRUE.equals(category.getActive()), 0, 0, List.of());
        }
        List<ClassificationOverviewResponse.CategoryNode> children =
                childrenByParent.getOrDefault(category.getId(), List.of()).stream()
                        .map(child -> toCategoryNode(child, childrenByParent, directCounts, new HashSet<>(path)))
                        .toList();
        long directCount = directCounts.getOrDefault(category.getId(), 0L);
        long totalCount = directCount + children.stream()
                .mapToLong(ClassificationOverviewResponse.CategoryNode::totalQuestionCount)
                .sum();
        return new ClassificationOverviewResponse.CategoryNode(
                category.getId(),
                category.getName(),
                Boolean.TRUE.equals(category.getActive()),
                directCount,
                totalCount,
                children);
    }

    private ClassificationOverviewResponse.CategoryItem toCategoryItem(
            Category category,
            Map<Long, Category> categoriesById,
            Map<Long, Long> directCounts) {
        Category parent = categoriesById.get(category.getParentId());
        return new ClassificationOverviewResponse.CategoryItem(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getParentId(),
                parent == null ? null : parent.getName(),
                Boolean.TRUE.equals(category.getActive()),
                category.getSortOrder(),
                directCounts.getOrDefault(category.getId(), 0L),
                category.getUpdatedAt());
    }

    private ClassificationOverviewResponse.TagItem toTagItem(Tag tag) {
        return new ClassificationOverviewResponse.TagItem(
                tag.getId(), tag.getName(), tag.getDescription(), tag.getUpdatedAt());
    }

    private void applyCategoryRequest(
            Category category,
            CategorySaveRequest request,
            String name,
            Category parent) {
        category.setName(name);
        category.setDescription(normalizeText(request.getDescription()));
        category.setParentId(parent == null ? null : parent.getId());
        category.setActive(request.getActive());
        category.setSortOrder(request.getSortOrder());
    }

    private Category resolveParent(Long parentId, Long userId) {
        return parentId == null ? null : findOwnedCategory(parentId, userId);
    }

    private void ensureNoCategoryCycle(Long categoryId, Category parent, Long userId) {
        Set<Long> visited = new HashSet<>();
        Category cursor = parent;
        while (cursor != null) {
            if (cursor.getId().equals(categoryId)) {
                throw new BusinessException("分类不能移动到自身或其子分类下");
            }
            if (!visited.add(cursor.getId()) || cursor.getParentId() == null) {
                break;
            }
            cursor = findOwnedCategory(cursor.getParentId(), userId);
        }
    }

    private void ensureCategoryNameAvailable(
            String name,
            Long parentId,
            Long userId,
            Long excludedCategoryId) {
        boolean duplicate = categoryRepository.findAllByCreatedByOrderBySortOrderAscNameAsc(userId).stream()
                .anyMatch(category -> !category.getId().equals(excludedCategoryId)
                        && java.util.Objects.equals(category.getParentId(), parentId)
                        && category.getName().equalsIgnoreCase(name));
        if (duplicate) {
            throw new BusinessException("同一层级下已存在同名分类");
        }
    }

    private Category findOwnedCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndCreatedBy(categoryId, userId)
                .orElseThrow(() -> new BusinessException("分类不存在或无权访问"));
    }

    private Tag findOwnedTag(Long tagId, Long userId) {
        return tagRepository.findByIdAndCreatedBy(tagId, userId)
                .orElseThrow(() -> new BusinessException("标签不存在或无权访问"));
    }

    private Map<Long, Category> categoryMap(Long userId) {
        Map<Long, Category> result = new LinkedHashMap<>();
        categoryRepository.findAllByCreatedByOrderBySortOrderAscNameAsc(userId)
                .forEach(category -> result.put(category.getId(), category));
        return result;
    }

    private Map<Long, Long> categoryQuestionCounts(Long userId) {
        Map<Long, Long> result = new HashMap<>();
        questionRepository.countQuestionsByCategory(userId).forEach(row ->
                result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue()));
        return result;
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("名称不能为空");
        }
        return value.trim();
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
