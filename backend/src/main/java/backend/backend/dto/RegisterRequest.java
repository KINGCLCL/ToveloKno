package backend.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求对象。
 *
 * 前端注册页面提交 JSON 时，会被 Spring 自动转换成这个对象。
 */
public class RegisterRequest {

    // @NotBlank 和 @Size 用来在进入业务逻辑前先做基础校验。
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3到50之间")
    private String username;

    // 密码长度先做最小限制，后面可以继续扩展强度校验。
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6到50之间")
    private String password;

    // 邮箱不是必填，但如果填写就必须符合邮箱格式。
    @Email(message = "邮箱格式不正确")
    private String email;

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
}
