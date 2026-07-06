package backend.backend.dto;

import java.time.LocalDateTime;

/**
 * 标签响应对象。
 */
public class TagResponse {

    private Long id;
    private String name;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;

    public TagResponse(Long id, String name, String description, Long createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
