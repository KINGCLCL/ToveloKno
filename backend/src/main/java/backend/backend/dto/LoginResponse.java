package backend.backend.dto;

/**
 * 登录成功响应对象。
 *
 * token 用于后续接口鉴权；user 用于前端展示当前用户基础信息。
 */
public class LoginResponse {

    // 登录凭证，前端需要保存并放到 Authorization 请求头。
    private String token;

    // token 类型，目前固定为 Bearer。
    private String tokenType;

    // 当前登录用户的安全展示信息，不包含密码。
    private UserResponse user;

    public LoginResponse(String token, String tokenType, UserResponse user) {
        this.token = token;
        this.tokenType = tokenType;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public UserResponse getUser() {
        return user;
    }
}
