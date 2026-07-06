package backend.backend.service;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.dto.CategoryRequest;
import backend.backend.dto.CategoryResponse;
import backend.backend.entity.Category;
import backend.backend.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分类模块业务层。
 *
 * 负责分类的新增、查询、修改、删除，以及父子分类关系校验。
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final OperationLogService operationLogService;

    public CategoryService(CategoryRepository categoryRepository, OperationLogService operationLogService) {
        this.categoryRepository = categoryRepository;
        this.operationLogService = operationLogService;
    }

    /**
     * 查询分类列表。
     *
     * keyword 为空时返回全部分类；不为空时按分类名模糊查询。
     */
    public List<CategoryResponse> listCategories(String keyword) {
        List<Category> categories = isBlank(keyword)
                ? categoryRepository.findAllByOrderByNameAsc()
                : categoryRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword.trim());
        return categories.stream().map(this::toResponse).toList();
    }

    /**
     * 查询分类详情。
     */
    public CategoryResponse getCategory(Long categoryId) {
        return toResponse(findCategoryById(categoryId));
    }

    /**
     * 新增分类。
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, AuthenticatedUser currentUser) {
        String name = normalizeRequiredText(request.getName(), "分类名称不能为空");
        String description = normalizeOptionalText(request.getDescription());
        Long parentId = normalizeParentId(request.getParentId());

        ensureCategoryNameAvailable(name, parentId, null);

        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setParentId(parentId);
        category.setCreatedBy(currentUser.getId());

        Category savedCategory = categoryRepository.save(category);
        operationLogService.record("CATEGORY_CREATE", "新增分类：" + savedCategory.getName());
        return toResponse(savedCategory);
    }

    /**
     * 修改分类。
     *
     * 修改父分类时会防止把分类挂到自己或自己的子孙分类下面。
     */
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        Category category = findCategoryById(categoryId);
        String name = normalizeRequiredText(request.getName(), "分类名称不能为空");
        String description = normalizeOptionalText(request.getDescription());
        Long parentId = normalizeParentId(request.getParentId());

        if (parentId != null && parentId.equals(categoryId)) {
            throw new IllegalArgumentException("父分类不能是自己");
        }
        if (parentId != null && isDescendant(parentId, categoryId)) {
            throw new IllegalArgumentException("不能把分类移动到自己的子分类下");
        }

        ensureCategoryNameAvailable(name, parentId, categoryId);

        category.setName(name);
        category.setDescription(description);
        category.setParentId(parentId);

        Category savedCategory = categoryRepository.save(category);
        operationLogService.record("CATEGORY_UPDATE", "修改分类：" + savedCategory.getName());
        return toResponse(savedCategory);
    }

    /**
     * 删除分类。
     *
     * 数据库外键已配置 ON DELETE SET NULL，关联资源、卡片、题目会自动解除分类关联。
     */
    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = findCategoryById(categoryId);
        categoryRepository.delete(category);
        operationLogService.record("CATEGORY_DELETE", "删除分类：" + category.getName());
    }

    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
    }

    private Long normalizeParentId(Long parentId) {
        if (parentId == null) {
            return null;
        }
        findCategoryById(parentId);
        return parentId;
    }

    private void ensureCategoryNameAvailable(String name, Long parentId, Long currentCategoryId) {
        boolean exists = currentCategoryId == null
                ? categoryRepository.existsByNameAndParentId(name, parentId)
                : categoryRepository.existsByNameAndParentIdAndIdNot(name, parentId, currentCategoryId);
        if (exists) {
            throw new IllegalArgumentException("同级分类名称已存在");
        }
    }

    private boolean isDescendant(Long possibleDescendantId, Long ancestorId) {
        Category current = findCategoryById(possibleDescendantId);
        while (current.getParentId() != null) {
            if (current.getParentId().equals(ancestorId)) {
                return true;
            }
            current = findCategoryById(current.getParentId());
        }
        return false;
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getParentId(),
                category.getCreatedBy(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private String normalizeRequiredText(String text, String message) {
        if (isBlank(text)) {
            throw new IllegalArgumentException(message);
        }
        return text.trim();
    }

    private String normalizeOptionalText(String text) {
        if (isBlank(text)) {
            return null;
        }
        return text.trim();
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
