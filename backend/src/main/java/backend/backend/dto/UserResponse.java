package backend.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应对象。
 *
 * 返回给前端的用户信息不包含 password，避免把加密后的密码暴露出去。
 */
public class UserResponse {

    // 用户基础信息，用于登录成功后的前端状态展示。
    private Long id;
    private String username;
    private String email;
    private String avatar;
    private Integer status;

    // 角色列表用于前端判断是否显示管理员菜单。
    private List<String> roles;

    // 注册时间可以展示在个人中心。
    private LocalDateTime createdAt;

    public UserResponse(Long id, String username, String email, String avatar, Integer status, List<String> roles, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.avatar = avatar;
        this.status = status;
        this.roles = roles;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatar() {
        return avatar;
    }

    public Integer getStatus() {
        return status;
    }

    public List<String> getRoles() {
        return roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
