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
    private String nickname;
    private String bio;
    private String avatar;
    private String profileBackground;
    private String profileSignature;
    private String profileCoverText;
    private String profileAge;
    private String profileOccupation;
    private String profilePhone;
    private String profileQq;
    private Integer status;

    // 角色列表用于前端判断是否显示管理员菜单。
    private List<String> roles;

    // 注册时间可以展示在个人中心。
    private LocalDateTime createdAt;

    public UserResponse(Long id, String username, String email, String nickname, String bio, String avatar, String profileBackground, String profileSignature, String profileCoverText, String profileAge, String profileOccupation, String profilePhone, String profileQq, Integer status, List<String> roles, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.nickname = nickname;
        this.bio = bio;
        this.avatar = avatar;
        this.profileBackground = profileBackground;
        this.profileSignature = profileSignature;
        this.profileCoverText = profileCoverText;
        this.profileAge = profileAge;
        this.profileOccupation = profileOccupation;
        this.profilePhone = profilePhone;
        this.profileQq = profileQq;
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

    public String getNickname() {
        return nickname;
    }

    public String getBio() {
        return bio;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getProfileBackground() {
        return profileBackground;
    }

    public String getProfileSignature() {
        return profileSignature;
    }

    public String getProfileCoverText() {
        return profileCoverText;
    }

    public String getProfileAge() {
        return profileAge;
    }

    public String getProfileOccupation() {
        return profileOccupation;
    }

    public String getProfilePhone() {
        return profilePhone;
    }

    public String getProfileQq() {
        return profileQq;
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
