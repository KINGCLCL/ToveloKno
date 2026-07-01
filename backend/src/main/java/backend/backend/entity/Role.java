package backend.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 角色实体类，对应数据库中的 role 表。
 *
 * 目前预置 USER 和 ADMIN 两种角色，用来区分普通学生用户和管理员。
 */
@Entity
@Table(name = "role")
public class Role {

    // 主键 id，数据库自动递增。
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 角色名称，例如 USER、ADMIN。
    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;

    // 角色说明，方便后台管理或数据库查看。
    @Column(length = 255)
    private String description;

    // 角色创建时间。
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
