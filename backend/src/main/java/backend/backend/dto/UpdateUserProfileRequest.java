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

    // 头像先保存 URL 或文件路径，长度限制和数据库字段保持一致。
    @Size(max = 255, message = "头像地址长度不能超过255")
    private String avatar;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
