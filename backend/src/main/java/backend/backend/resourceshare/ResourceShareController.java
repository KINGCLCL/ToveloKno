package backend.backend.resourceshare;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import backend.backend.resourceshare.ResourceShareDtos.SharedResourceRequest;
import backend.backend.resourceshare.ResourceShareDtos.SharedResourceResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/shared-resources")
public class ResourceShareController {
    private final ResourceShareService resourceShareService;

    public ResourceShareController(ResourceShareService resourceShareService) {
        this.resourceShareService = resourceShareService;
    }

    @GetMapping
    public ApiResponse<List<SharedResourceResponse>> list(@CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", resourceShareService.list(currentUser));
    }

    @PostMapping
    public ApiResponse<SharedResourceResponse> create(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestPart("data") SharedResourceRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.success("发布成功", resourceShareService.create(currentUser, request, file));
    }

    @PutMapping("/{resourceId}/view")
    public ApiResponse<SharedResourceResponse> view(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long resourceId) {
        return ApiResponse.success("已记录浏览", resourceShareService.view(resourceId, currentUser));
    }

    @PutMapping("/{resourceId}/favorite")
    public ApiResponse<SharedResourceResponse> toggleFavorite(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long resourceId) {
        return ApiResponse.success("收藏已更新", resourceShareService.toggleFavorite(resourceId, currentUser));
    }
}
