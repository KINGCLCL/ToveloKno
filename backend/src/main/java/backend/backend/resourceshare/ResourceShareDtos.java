package backend.backend.resourceshare;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class ResourceShareDtos {
    private ResourceShareDtos() {
    }

    public record SharedResourceRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 40) String category,
            @NotBlank @Size(max = 40) String kind,
            @Size(max = 800) String description,
            @Size(max = 300) String tags,
            @Size(max = 500) String linkUrl,
            String coverDataUrl) {
    }

    public record SharedResourceResponse(
            Long id,
            Long ownerId,
            String author,
            String title,
            String category,
            String kind,
            String description,
            List<String> tags,
            String link,
            String fileUrl,
            String cover,
            String originalFilename,
            Long fileSize,
            String duration,
            int views,
            long favorites,
            boolean favorite,
            boolean uploaded,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
