package backend.backend.auth;

import java.util.List;

/**
 * 当前请求中已经通过登录校验的用户信息。
 *
 * 这个对象只保存业务接口常用的身份字段，不直接暴露 User 实体，
 * 避免 Controller 层误用密码、状态等敏感字段。
 */
public class AuthenticatedUser {

    private final Long id;
    private final String username;
    private final List<String> roles;

    public AuthenticatedUser(Long id, String username, List<String> roles) {
        this.id = id;
        this.username = username;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }

    /**
     * 判断当前用户是否拥有指定角色。
     *
     * 业务模块里需要区分普通用户和管理员时，可以直接调用这个方法。
     */
    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(role -> role.equalsIgnoreCase(roleName));
    }
}
