package backend.backend.classification;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.common.BusinessException;
import backend.backend.question.Question;
import backend.backend.question.QuestionRepository;
import backend.backend.service.OperationLogService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassificationServiceTests {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private OperationLogService operationLogService;

    private ClassificationService classificationService;
    private AuthenticatedUser currentUser;

    @BeforeEach
    void setUp() {
        classificationService = new ClassificationService(
                categoryRepository,
                tagRepository,
                questionRepository,
                operationLogService);
        currentUser = new AuthenticatedUser(7L, "tester", List.of("USER"));
    }

    @Test
    void createCategoryStoresOwnerAndTreeSettings() {
        when(categoryRepository.findAllByCreatedByOrderBySortOrderAscNameAsc(7L))
                .thenReturn(List.of())
                .thenAnswer(invocation -> List.of());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(12L);
            category.prePersist();
            return category;
        });
        when(questionRepository.countQuestionsByCategory(7L)).thenReturn(List.of());

        ClassificationOverviewResponse.CategoryItem result =
                classificationService.createCategory(categoryRequest("数据库"), currentUser);

        assertThat(result.id()).isEqualTo(12L);
        assertThat(result.name()).isEqualTo("数据库");
        assertThat(result.active()).isTrue();
        verify(operationLogService).record(7L, "CATEGORY_CREATE", "新增分类：数据库");
    }

    @Test
    void updateCategoryRejectsMovingBelowItself() {
        Category category = category(12L, "数据库", null);
        when(categoryRepository.findByIdAndCreatedBy(12L, 7L)).thenReturn(Optional.of(category));

        CategorySaveRequest request = categoryRequest("数据库");
        request.setParentId(12L);

        assertThatThrownBy(() -> classificationService.updateCategory(12L, request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("分类不能移动到自身或其子分类下");
    }

    @Test
    void deleteCategoryWithChildrenIsRejected() {
        Category category = category(12L, "数据库", null);
        when(categoryRepository.findByIdAndCreatedBy(12L, 7L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByCreatedByAndParentId(7L, 12L)).thenReturn(true);

        assertThatThrownBy(() -> classificationService.deleteCategory(12L, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该分类下还有子分类，请先移动或删除子分类");
    }

    @Test
    void renameKnowledgePointUpdatesEveryMatchingQuestion() {
        Question first = new Question();
        first.setKnowledgePoint("事务");
        Question second = new Question();
        second.setKnowledgePoint("事务");
        when(questionRepository.findByKnowledgePoint(7L, "事务", "数据库"))
                .thenReturn(List.of(first, second));

        KnowledgePointRenameRequest request = new KnowledgePointRenameRequest();
        request.setOldName("事务");
        request.setNewName("事务管理");
        request.setSubject("数据库");

        int updated = classificationService.renameKnowledgePoint(request, currentUser);

        assertThat(updated).isEqualTo(2);
        assertThat(first.getKnowledgePoint()).isEqualTo("事务管理");
        assertThat(second.getKnowledgePoint()).isEqualTo("事务管理");
        verify(questionRepository).saveAll(List.of(first, second));
    }

    @Test
    void categoryFilterIncludesEveryDescendant() {
        Category root = category(1L, "计算机类", null);
        Category child = category(2L, "数据库", 1L);
        Category grandchild = category(3L, "事务管理", 2L);
        Category unrelated = category(4L, "高等数学", null);
        when(categoryRepository.findByIdAndCreatedBy(1L, 7L)).thenReturn(Optional.of(root));
        when(categoryRepository.findAllByCreatedByOrderBySortOrderAscNameAsc(7L))
                .thenReturn(List.of(root, child, grandchild, unrelated));

        assertThat(classificationService.getOwnedCategoryAndDescendantIds(1L, 7L))
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    private CategorySaveRequest categoryRequest(String name) {
        CategorySaveRequest request = new CategorySaveRequest();
        request.setName(name);
        request.setDescription("课程分类");
        request.setActive(true);
        request.setSortOrder(10);
        return request;
    }

    private Category category(Long id, String name, Long parentId) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setParentId(parentId);
        category.setActive(true);
        category.setSortOrder(0);
        category.setCreatedBy(7L);
        return category;
    }
}
