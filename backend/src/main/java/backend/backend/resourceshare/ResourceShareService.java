package backend.backend.resourceshare;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.common.BusinessException;
import backend.backend.entity.User;
import backend.backend.repository.UserRepository;
import backend.backend.resourceshare.ResourceShareDtos.SharedResourceRequest;
import backend.backend.resourceshare.ResourceShareDtos.SharedResourceResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResourceShareService {
    private static final Path UPLOAD_DIR = Path.of("uploads", "shared-resources");

    private final SharedResourceRepository resourceRepository;
    private final SharedResourceFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    public ResourceShareService(
            SharedResourceRepository resourceRepository,
            SharedResourceFavoriteRepository favoriteRepository,
            UserRepository userRepository) {
        this.resourceRepository = resourceRepository;
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
    }

    public List<SharedResourceResponse> list(AuthenticatedUser currentUser) {
        Long userId = currentUser.getId();
        Set<Long> favoriteIds = favoriteRepository.findAllByUserId(userId).stream()
                .map(SharedResourceFavorite::getResourceId)
                .collect(Collectors.toSet());
        List<SharedResource> resources = resourceRepository.findAllByOrderByUpdatedAtDesc();
        Map<Long, User> users = userRepository.findAllById(resources.stream().map(SharedResource::getOwnerId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return resources.stream()
                .map(resource -> toResponse(resource, users.get(resource.getOwnerId()), favoriteIds.contains(resource.getId()), true))
                .toList();
    }

    public SharedResourceResponse create(AuthenticatedUser currentUser, SharedResourceRequest request, MultipartFile file) {
        LocalDateTime now = LocalDateTime.now();
        SharedResource resource = new SharedResource();
        resource.setOwnerId(currentUser.getId());
        resource.setTitle(clean(request.title(), 120));
        resource.setCategory(clean(request.category(), 40));
        resource.setKind(clean(request.kind(), 40));
        resource.setDescription(clean(request.description(), 800));
        resource.setTags(clean(request.tags(), 300));
        resource.setLinkUrl(clean(request.linkUrl(), 500));
        resource.setViewCount(0);
        resource.setLikeCount(0);
        resource.setCreatedAt(now);
        resource.setUpdatedAt(now);

        if (file != null && !file.isEmpty()) {
            storeFile(resource, file);
        }
        storeCover(resource, request.coverDataUrl());

        SharedResource saved = resourceRepository.save(resource);
        User author = userRepository.findById(currentUser.getId()).orElse(null);
        return toResponse(saved, author, false, true);
    }

    public SharedResourceResponse view(Long resourceId, AuthenticatedUser currentUser) {
        SharedResource resource = findResource(resourceId);
        resource.setViewCount(resource.getViewCount() + 1);
        resource.setUpdatedAt(LocalDateTime.now());
        SharedResource saved = resourceRepository.save(resource);
        boolean favorite = favoriteRepository.findByUserIdAndResourceId(currentUser.getId(), resourceId).isPresent();
        User author = userRepository.findById(saved.getOwnerId()).orElse(null);
        return toResponse(saved, author, favorite, true);
    }

    public SharedResourceResponse toggleFavorite(Long resourceId, AuthenticatedUser currentUser) {
        SharedResource resource = findResource(resourceId);
        boolean favorite;
        var existing = favoriteRepository.findByUserIdAndResourceId(currentUser.getId(), resourceId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            favorite = false;
        } else {
            SharedResourceFavorite next = new SharedResourceFavorite();
            next.setUserId(currentUser.getId());
            next.setResourceId(resourceId);
            next.setCreatedAt(LocalDateTime.now());
            favoriteRepository.save(next);
            favorite = true;
        }
        User author = userRepository.findById(resource.getOwnerId()).orElse(null);
        return toResponse(resource, author, favorite, true);
    }

    private SharedResource findResource(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException("资源不存在"));
    }

    private void storeFile(SharedResource resource, MultipartFile file) {
        try {
            Files.createDirectories(UPLOAD_DIR);
            String originalName = file.getOriginalFilename() == null ? "shared-resource" : file.getOriginalFilename();
            String extension = "";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                extension = originalName.substring(dot);
            }
            String storedName = UUID.randomUUID() + extension;
            Files.copy(file.getInputStream(), UPLOAD_DIR.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            resource.setOriginalFilename(originalName);
            resource.setFileSize(file.getSize());
            resource.setFileUrl("/uploads/shared-resources/" + storedName);
            if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
                resource.setCoverUrl(resource.getFileUrl());
            }
            if (resource.getLinkUrl() == null || resource.getLinkUrl().isBlank()) {
                resource.setLinkUrl(resource.getFileUrl());
            }
        } catch (IOException exception) {
            throw new BusinessException("资源文件保存失败");
        }
    }

    private void storeCover(SharedResource resource, String coverDataUrl) {
        if (coverDataUrl == null || coverDataUrl.isBlank()) return;
        String marker = ";base64,";
        int markerIndex = coverDataUrl.indexOf(marker);
        if (!coverDataUrl.startsWith("data:image/") || markerIndex < 0) return;
        try {
            Files.createDirectories(UPLOAD_DIR);
            String meta = coverDataUrl.substring(5, markerIndex);
            String extension = meta.contains("png") ? ".png" : ".jpg";
            byte[] bytes = Base64.getDecoder().decode(coverDataUrl.substring(markerIndex + marker.length()));
            if (bytes.length > 2 * 1024 * 1024) {
                throw new BusinessException("视频封面不能超过 2MB");
            }
            String storedName = UUID.randomUUID() + "-cover" + extension;
            Files.write(UPLOAD_DIR.resolve(storedName), bytes);
            resource.setCoverUrl("/uploads/shared-resources/" + storedName);
        } catch (IllegalArgumentException | IOException exception) {
            throw new BusinessException("视频封面保存失败");
        }
    }

    private SharedResourceResponse toResponse(SharedResource resource, User author, boolean favorite, boolean uploaded) {
        String authorName = author == null
                ? "学习用户"
                : !blank(author.getNickname()) ? author.getNickname() : author.getUsername();
        String link = !blank(resource.getFileUrl()) ? resource.getFileUrl() : resource.getLinkUrl();
        return new SharedResourceResponse(
                resource.getId(),
                resource.getOwnerId(),
                authorName,
                resource.getTitle(),
                resource.getCategory(),
                resource.getKind(),
                resource.getDescription(),
                splitTags(resource.getTags()),
                link,
                resource.getFileUrl(),
                resource.getCoverUrl(),
                resource.getOriginalFilename(),
                resource.getFileSize(),
                durationText(resource),
                resource.getViewCount() == null ? 0 : resource.getViewCount(),
                favoriteRepository.countByResourceId(resource.getId()),
                favorite,
                uploaded,
                resource.getCreatedAt(),
                resource.getUpdatedAt());
    }

    private String durationText(SharedResource resource) {
        if ("视频".equals(resource.getKind())) return "视频";
        if (resource.getFileSize() != null) return sizeText(resource.getFileSize());
        return "链接".equals(resource.getKind()) ? "链接" : resource.getKind();
    }

    private String sizeText(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) return "%.1f GB".formatted(bytes / 1024d / 1024 / 1024);
        if (bytes >= 1024L * 1024) return "%.1f MB".formatted(bytes / 1024d / 1024);
        if (bytes >= 1024L) return "%.1f KB".formatted(bytes / 1024d);
        return bytes + " B";
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) return List.of("分享");
        return Arrays.stream(tags.split("[,，]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .limit(6)
                .toList();
    }

    private String clean(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
