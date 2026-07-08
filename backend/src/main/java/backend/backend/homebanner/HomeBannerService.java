package backend.backend.homebanner;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.homebanner.HomeBannerDtos.HomeBannerItemRequest;
import backend.backend.homebanner.HomeBannerDtos.HomeBannerResponse;
import backend.backend.homebanner.HomeBannerDtos.HomeBannerSaveRequest;
import backend.backend.homebanner.HomeBannerDtos.HomeBannerUploadResponse;
import backend.backend.service.OperationLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HomeBannerService {

    private static final List<HomeBannerResponse> DEFAULT_BANNERS = List.of(
            new HomeBannerResponse("continue-resource", "从星标资料继续", "", "", 0, null),
            new HomeBannerResponse("today-plan", "把任务推进到下一格", "", "", 1, null),
            new HomeBannerResponse("wrong-review", "错题优先处理", "", "", 2, null),
            new HomeBannerResponse("question-bank", "进入练习模块", "", "", 3, null)
    );

    private final HomeBannerRepository homeBannerRepository;
    private final OperationLogService operationLogService;
    private final Path uploadRoot;

    public HomeBannerService(
            HomeBannerRepository homeBannerRepository,
            OperationLogService operationLogService,
            @Value("${app.upload.banner-dir:uploads/banners}") String uploadDir) {
        this.homeBannerRepository = homeBannerRepository;
        this.operationLogService = operationLogService;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<HomeBannerResponse> listBanners() {
        List<HomeBanner> saved = homeBannerRepository.findAllByOrderBySortOrderAscIdAsc();
        if (saved.isEmpty()) {
            return DEFAULT_BANNERS;
        }
        return saved.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<HomeBannerResponse> saveBanners(HomeBannerSaveRequest request, AuthenticatedUser currentUser) {
        Map<String, HomeBanner> existing = homeBannerRepository.findAll().stream()
                .collect(Collectors.toMap(HomeBanner::getBannerKey, Function.identity()));

        for (int index = 0; index < request.banners().size(); index++) {
            HomeBannerItemRequest item = request.banners().get(index);
            String key = normalizeKey(item.id(), index);
            HomeBanner banner = existing.getOrDefault(key, new HomeBanner());
            banner.setBannerKey(key);
            banner.setTitle(normalizeText(item.title(), 150, defaultTitle(index)));
            banner.setText(normalizeText(item.text(), 300, ""));
            banner.setImageUrl(normalizeText(item.imageUrl(), 500, ""));
            banner.setSortOrder(index);
            banner.setUpdatedBy(currentUser.getId());
            homeBannerRepository.save(banner);
        }

        operationLogService.record(currentUser.getId(), "HOME_BANNER_SAVE", "保存首页推荐画幅");
        return listBanners();
    }

    public HomeBannerUploadResponse uploadBannerImage(MultipartFile file, AuthenticatedUser currentUser) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("只能上传图片文件");
        }
        if (file.getSize() > 8 * 1024 * 1024) {
            throw new IllegalArgumentException("图片不能超过 8MB");
        }

        String filename = UUID.randomUUID() + extensionFrom(file.getOriginalFilename(), contentType);
        try {
            Files.createDirectories(uploadRoot);
            file.transferTo(uploadRoot.resolve(filename));
        } catch (IOException exception) {
            throw new IllegalStateException("画幅图片保存失败");
        }
        operationLogService.record(currentUser.getId(), "HOME_BANNER_UPLOAD", "上传首页推荐画幅图片");
        return new HomeBannerUploadResponse("/uploads/banners/" + filename);
    }

    @Transactional
    public List<HomeBannerResponse> resetBanners(AuthenticatedUser currentUser) {
        homeBannerRepository.deleteAll();
        operationLogService.record(currentUser.getId(), "HOME_BANNER_RESET", "恢复首页推荐画幅默认值");
        return DEFAULT_BANNERS;
    }

    private HomeBannerResponse toResponse(HomeBanner banner) {
        return new HomeBannerResponse(
                banner.getBannerKey(),
                banner.getTitle(),
                banner.getText(),
                banner.getImageUrl(),
                banner.getSortOrder(),
                banner.getUpdatedAt()
        );
    }

    private String normalizeKey(String value, int index) {
        String fallback = DEFAULT_BANNERS.get(Math.min(index, DEFAULT_BANNERS.size() - 1)).id();
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80);
    }

    private String defaultTitle(int index) {
        return DEFAULT_BANNERS.get(Math.min(index, DEFAULT_BANNERS.size() - 1)).title();
    }

    private String normalizeText(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String extensionFrom(String originalFilename, String contentType) {
        Set<String> allowed = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                String extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
                if (allowed.contains(extension)) {
                    return extension;
                }
            }
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
