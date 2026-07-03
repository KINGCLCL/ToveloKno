package backend.backend.controller;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import backend.backend.dto.CategoryRequest;
import backend.backend.dto.CategoryResponse;
import backend.backend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类管理接口。
 *
 * 分类是资源、知识卡片、题目模块都会用到的公共基础数据。
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 查询分类列表，支持按名称关键词搜索。
     */
    @GetMapping
    public ApiResponse<List<CategoryResponse>> listCategories(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success("查询成功", categoryService.listCategories(keyword));
    }

    /**
     * 查询分类详情。
     */
    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> getCategory(@PathVariable Long categoryId) {
        return ApiResponse.success("查询成功", categoryService.getCategory(categoryId));
    }

    /**
     * 新增分类。
     */
    @PostMapping
    public ApiResponse<CategoryResponse> createCategory(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success("新增成功", categoryService.createCategory(request, currentUser));
    }

    /**
     * 修改分类。
     */
    @PutMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success("修改成功", categoryService.updateCategory(categoryId, request));
    }

    /**
     * 删除分类。
     */
    @DeleteMapping("/{categoryId}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ApiResponse.success("删除成功", null);
    }
}
