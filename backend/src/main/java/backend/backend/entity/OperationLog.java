package backend.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 操作日志实体，对应 operation_log 表。
 *
 * 用于记录用户登录、修改资料、新增题目、完成学习计划等关键操作，
 * 后续后台管理或问题排查可以基于这张表查看行为轨迹。
 */
@Entity
@Table(name = "operation_log")
public class OperationLog {

    // 主键 id，数据库自动递增。
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 操作人 id，允许为空，方便记录系统级操作。
    @Column(name = "user_id")
    private Long userId;

    // 操作类型，例如 USER_LOGIN、QUESTION_CREATE。
    @Column(name = "operation_type", nullable = false, length = 80)
    private String operationType;

    // 操作内容摘要，保存给管理员或开发者查看。
    @Column(name = "operation_content", length = 500)
    private String operationContent;

    // 客户端 IP 地址。
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    // 日志创建时间。
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 入库前自动写入创建时间，避免业务层每次手动设置。
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationContent() {
        return operationContent;
    }

    public void setOperationContent(String operationContent) {
        this.operationContent = operationContent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
