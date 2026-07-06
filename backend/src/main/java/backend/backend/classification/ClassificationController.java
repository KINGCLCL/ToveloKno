package backend.backend.classification;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/classifications")
public class ClassificationController {

    private final ClassificationService classificationService;

    public ClassificationController(ClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    @GetMapping("/overview")
    public ApiResponse<ClassificationOverviewResponse> getOverview(
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", classificationService.getOverview(currentUser));
    }

    @PostMapping("/categories")
    public ApiResponse<ClassificationOverviewResponse.CategoryItem> createCategory(
            @Valid @RequestBody CategorySaveRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("分类创建成功", classificationService.createCategory(request, currentUser));
    }

    @PutMapping("/categories/{categoryId}")
    public ApiResponse<ClassificationOverviewResponse.CategoryItem> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategorySaveRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success(
                "分类修改成功",
                classificationService.updateCategory(categoryId, request, currentUser));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ApiResponse<Void> deleteCategory(
            @PathVariable Long categoryId,
            @CurrentUser AuthenticatedUser currentUser) {
        classificationService.deleteCategory(categoryId, currentUser);
        return ApiResponse.success("分类删除成功", null);
    }

    @PostMapping("/tags")
    public ApiResponse<ClassificationOverviewResponse.TagItem> createTag(
            @Valid @RequestBody TagSaveRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("标签创建成功", classificationService.createTag(request, currentUser));
    }

    @PutMapping("/tags/{tagId}")
    public ApiResponse<ClassificationOverviewResponse.TagItem> updateTag(
            @PathVariable Long tagId,
            @Valid @RequestBody TagSaveRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("标签修改成功", classificationService.updateTag(tagId, request, currentUser));
    }

    @DeleteMapping("/tags/{tagId}")
    public ApiResponse<Void> deleteTag(
            @PathVariable Long tagId,
            @CurrentUser AuthenticatedUser currentUser) {
        classificationService.deleteTag(tagId, currentUser);
        return ApiResponse.success("标签删除成功", null);
    }

    @PutMapping("/knowledge-points")
    public ApiResponse<Integer> renameKnowledgePoint(
            @Valid @RequestBody KnowledgePointRenameRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success(
                "知识点修改成功",
                classificationService.renameKnowledgePoint(request, currentUser));
    }

    @DeleteMapping("/knowledge-points")
    public ApiResponse<Integer> clearKnowledgePoint(
            @RequestParam @Size(max = 80, message = "知识点名称不能超过80个字符") String name,
            @RequestParam(required = false) @Size(max = 80, message = "科目名称不能超过80个字符") String subject,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success(
                "知识点已从题目中移除",
                classificationService.clearKnowledgePoint(name, subject, currentUser));
    }
}
