package backend.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求对象。
 *
 * 只接收用户名和密码，不接收角色、状态等敏感字段。
 */
public class LoginRequest {

    // 用户名不能为空，否则没有办法查找账号。
    @NotBlank(message = "用户名不能为空")
    private String username;

    // 密码不能为空，实际比较会在 UserService 中完成。
    @NotBlank(message = "密码不能为空")
    private String password;

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
}
