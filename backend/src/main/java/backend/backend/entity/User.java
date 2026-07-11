package backend.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户实体类，对应数据库中的 user 表。
 *
 * 实体类的作用是把数据库表结构映射成 Java 对象，后续注册、登录、
 * 资料上传、学习计划等功能都会通过 user_id 关联到这个表。
 */
@Entity
@Table(name = "user")
public class User {

    // 主键 id，数据库自动递增。
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 用户名用于登录，设置 unique 可以避免重复注册。
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // 密码保存的是 BCrypt 加密后的结果，不保存明文密码。
    @Column(nullable = false, length = 255)
    private String password;

    // 邮箱当前用于用户资料展示，后面也可以扩展成找回密码。
    @Column(unique = true, length = 100)
    private String email;

    // 个人主页展示昵称。
    @Column(length = 80)
    private String nickname;

    // 个人主页简介。
    @Column(length = 500)
    private String bio;

    // 头像地址，先保存字符串路径或 URL。
    @Column(length = 255)
    private String avatar;

    // 个人主页背景图地址，保存上传后的静态访问路径。
    @Column(name = "profile_background", length = 255)
    private String profileBackground;

    // 个人主页短签名。
    @Column(name = "profile_signature", length = 160)
    private String profileSignature;

    // 个人主页头图状态文案。
    @Column(name = "profile_cover_text", length = 160)
    private String profileCoverText;

    // 个人主页扩展信息。
    @Column(name = "profile_age", length = 20)
    private String profileAge;

    @Column(name = "profile_occupation", length = 80)
    private String profileOccupation;

    @Column(name = "profile_phone", length = 40)
    private String profilePhone;

    @Column(name = "profile_qq", length = 40)
    private String profileQq;

    // 账号状态：1 正常，0 禁用。
    @Column(nullable = false)
    private Integer status = 1;

    // 创建时间，由 JPA 在保存前自动填充。
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 更新时间，由 JPA 在新增和修改时自动维护。
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 用户和角色是多对多关系，中间表是 user_role。
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // 第一次保存用户前自动写入创建时间和更新时间。
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    // 用户资料被修改时自动刷新更新时间。
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getProfileBackground() {
        return profileBackground;
    }

    public void setProfileBackground(String profileBackground) {
        this.profileBackground = profileBackground;
    }

    public String getProfileSignature() {
        return profileSignature;
    }

    public void setProfileSignature(String profileSignature) {
        this.profileSignature = profileSignature;
    }

    public String getProfileCoverText() {
        return profileCoverText;
    }

    public void setProfileCoverText(String profileCoverText) {
        this.profileCoverText = profileCoverText;
    }

    public String getProfileAge() {
        return profileAge;
    }

    public void setProfileAge(String profileAge) {
        this.profileAge = profileAge;
    }

    public String getProfileOccupation() {
        return profileOccupation;
    }

    public void setProfileOccupation(String profileOccupation) {
        this.profileOccupation = profileOccupation;
    }

    public String getProfilePhone() {
        return profilePhone;
    }

    public void setProfilePhone(String profilePhone) {
        this.profilePhone = profilePhone;
    }

    public String getProfileQq() {
        return profileQq;
    }

    public void setProfileQq(String profileQq) {
        this.profileQq = profileQq;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
