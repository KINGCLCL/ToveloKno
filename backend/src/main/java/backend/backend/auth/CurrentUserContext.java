package backend.backend.auth;

import backend.backend.common.UnauthorizedException;

/**
 * 保存一次 HTTP 请求里的当前用户。
 *
 * 拦截器在请求开始时写入用户信息，请求结束后清理。
 * ThreadLocal 可以避免把 currentUser 在 Service 方法之间层层传递。
 */
public final class CurrentUserContext {

    private static final ThreadLocal<AuthenticatedUser> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(AuthenticatedUser user) {
        CURRENT_USER.set(user);
    }

    /**
     * 读取当前登录用户。
     *
     * 如果接口没有经过登录校验，说明调用方式不对，统一抛出未登录异常。
     */
    public static AuthenticatedUser get() {
        AuthenticatedUser user = CURRENT_USER.get();
        if (user == null) {
            throw new UnauthorizedException("请先登录");
        }
        return user;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
