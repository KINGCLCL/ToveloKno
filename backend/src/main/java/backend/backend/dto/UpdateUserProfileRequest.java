package backend.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 修改用户资料请求对象。
 *
 * 当前先支持修改邮箱和头像，后面如果加昵称、简介，也可以继续扩展这个类。
 */
public class UpdateUserProfileRequest {

    // 邮箱允许不填；如果填写，就必须符合邮箱格式。
    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 80, message = "昵称长度不能超过80")
    private String nickname;

    @Size(max = 500, message = "个人简介不能超过500字")
    private String bio;

    // 头像先保存 URL 或文件路径，长度限制和数据库字段保持一致。
    @Size(max = 255, message = "头像地址长度不能超过255")
    private String avatar;

    // 个人主页背景图地址。
    @Size(max = 255, message = "主页背景地址长度不能超过255")
    private String profileBackground;

    @Size(max = 160, message = "签名不能超过160字")
    private String profileSignature;

    @Size(max = 160, message = "头图文案不能超过160字")
    private String profileCoverText;

    @Size(max = 20, message = "年龄不能超过20个字符")
    private String profileAge;

    @Size(max = 80, message = "职业不能超过80个字符")
    private String profileOccupation;

    @Size(max = 40, message = "电话不能超过40个字符")
    private String profilePhone;

    @Size(max = 40, message = "QQ不能超过40个字符")
    private String profileQq;

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
}
