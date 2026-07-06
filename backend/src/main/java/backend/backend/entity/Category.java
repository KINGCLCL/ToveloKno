package backend.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 分类实体，对应数据库 category 表。
 *
 * 分类用于给学习资源、知识卡片、题目按学科或课程分组。
 * 这里用 parentId 表示父分类，方便后续做“高数/极限”这种层级分类。
 */
@Entity
@Table(name = "category")
public class Category {

    // 主键 id，数据库自动递增。
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 分类名称，同一个父分类下不允许重复。
    @Column(nullable = false, length = 80)
    private String name;

    // 分类说明，可用于展示分类用途。
    @Column(length = 255)
    private String description;

    // 父分类 id，null 表示一级分类。
    @Column(name = "parent_id")
    private Long parentId;

    // 创建人 id，保留给后续管理员审计或个性化分类使用。
    @Column(name = "created_by")
    private Long createdBy;

    // 创建时间。
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 更新时间。
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
