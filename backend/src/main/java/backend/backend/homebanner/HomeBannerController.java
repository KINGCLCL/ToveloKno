package backend.backend.homebanner;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import backend.backend.homebanner.HomeBannerDtos.HomeBannerResponse;
import backend.backend.homebanner.HomeBannerDtos.HomeBannerSaveRequest;
import backend.backend.homebanner.HomeBannerDtos.HomeBannerUploadResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/home-banners")
public class HomeBannerController {

    private final HomeBannerService homeBannerService;

    public HomeBannerController(HomeBannerService homeBannerService) {
        this.homeBannerService = homeBannerService;
    }

    @GetMapping
    public ApiResponse<List<HomeBannerResponse>> listBanners() {
        return ApiResponse.success("查询成功", homeBannerService.listBanners());
    }

    @PutMapping
    public ApiResponse<List<HomeBannerResponse>> saveBanners(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody HomeBannerSaveRequest request) {
        return ApiResponse.success("保存成功", homeBannerService.saveBanners(request, currentUser));
    }

    @PostMapping("/image")
    public ApiResponse<HomeBannerUploadResponse> uploadBannerImage(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("上传成功", homeBannerService.uploadBannerImage(file, currentUser));
    }

    @DeleteMapping
    public ApiResponse<List<HomeBannerResponse>> resetBanners(@CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("已恢复默认", homeBannerService.resetBanners(currentUser));
    }
}
