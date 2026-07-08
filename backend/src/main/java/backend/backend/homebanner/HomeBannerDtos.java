package backend.backend.homebanner;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class HomeBannerDtos {
    private HomeBannerDtos() {
    }

    public record HomeBannerResponse(
            String id,
            String title,
            String text,
            String imageUrl,
            Integer sortOrder,
            LocalDateTime updatedAt) {
    }

    public record HomeBannerItemRequest(
            @Size(max = 80) String id,
            @Size(max = 150) String title,
            @Size(max = 300) String text,
            @Size(max = 500) String imageUrl) {
    }

    public record HomeBannerSaveRequest(
            @NotNull @Size(min = 1, max = 8) List<@Valid HomeBannerItemRequest> banners) {
    }

    public record HomeBannerUploadResponse(String imageUrl) {
    }
}
