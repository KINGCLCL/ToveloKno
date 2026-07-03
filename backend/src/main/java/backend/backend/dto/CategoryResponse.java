package backend.backend.dto;

import java.time.LocalDateTime;

/**
 * 分类响应对象。
 *
 * 返回给前端展示分类列表、下拉选择、分类详情。
 */
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CategoryResponse(
            Long id,
            String name,
            String description,
            Long parentId,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.parentId = parentId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Long getParentId() {
        return parentId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
