package backend.backend.learningresource;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import backend.backend.common.PageResponse;
import backend.backend.learningresource.LearningResourceDtos.AnnotationsRequest;
import backend.backend.learningresource.LearningResourceDtos.ProgressRequest;
import backend.backend.learningresource.LearningResourceDtos.ResourceResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/resources")
public class LearningResourceController {

    private final LearningResourceService learningResourceService;

    public LearningResourceController(LearningResourceService learningResourceService) {
        this.learningResourceService = learningResourceService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ResourceResponse>> listResources(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return ApiResponse.success(
                "查询成功",
                learningResourceService.listResources(currentUser, keyword, type, favorite, page, size)
        );
    }

    @GetMapping("/{resourceId}")
    public ApiResponse<ResourceResponse> getResource(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long resourceId) {
        return ApiResponse.success("查询成功", learningResourceService.getResource(resourceId, currentUser));
    }

    @PostMapping
    public ApiResponse<ResourceResponse> uploadResource(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description) {
        return ApiResponse.success(
                "上传成功",
                learningResourceService.uploadResource(currentUser, file, name, description)
        );
    }

    @PutMapping("/{resourceId}/progress")
    public ApiResponse<ResourceResponse> updateProgress(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long resourceId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ProgressRequest request) {
        return ApiResponse.success(
                "进度已保存",
                learningResourceService.updateProgress(resourceId, currentUser, request)
        );
    }

    @PutMapping("/{resourceId}/annotations")
    public ApiResponse<ResourceResponse> updateAnnotations(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long resourceId,
            @Valid @org.springframework.web.bind.annotation.RequestBody AnnotationsRequest request) {
        return ApiResponse.success(
                "标注已保存",
                learningResourceService.updateAnnotations(resourceId, currentUser, request.annotations())
        );
    }

    @PutMapping("/{resourceId}/favorite")
    public ApiResponse<ResourceResponse> toggleFavorite(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long resourceId) {
        return ApiResponse.success(
                "星标已更新",
                learningResourceService.toggleFavorite(resourceId, currentUser)
        );
    }

    @DeleteMapping("/{resourceId}")
    public ApiResponse<Void> deleteResource(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long resourceId) {
        learningResourceService.deleteResource(resourceId, currentUser);
        return ApiResponse.success("删除成功", null);
    }
}
