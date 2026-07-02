package backend.backend.auth;

import backend.backend.common.UnauthorizedException;
import backend.backend.entity.User;
import backend.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 统一拦截 /api/** 请求并校验登录态。
 *
 * 注册、登录、健康检查和浏览器预检请求会放行；
 * 其他业务接口必须携带 Authorization: Bearer token。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthTokenService authTokenService;
    private final UserRepository userRepository;

    public AuthInterceptor(AuthTokenService authTokenService, UserRepository userRepository) {
        this.authTokenService = authTokenService;
        this.userRepository = userRepository;
    }

    /**
     * 请求进入 Controller 之前执行。
     *
     * 这里既验证 token，也重新从数据库读取用户状态，确保禁用账号无法继续访问接口。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPublicRequest(request)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("请先登录");
        }

        AuthenticatedUser tokenUser = authTokenService.parseToken(authorization.substring(7));
        User user = userRepository.findById(tokenUser.getId())
                .orElseThrow(() -> new UnauthorizedException("登录用户不存在"));
        if (user.getStatus() == 0) {
            throw new UnauthorizedException("账号已被禁用");
        }

        CurrentUserContext.set(new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getRoles().stream().map(role -> role.getRoleName()).sorted().toList()
        ));
        return true;
    }

    /**
     * 请求结束后清理 ThreadLocal，避免线程复用时串到下一个请求。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }

    // 这些接口不需要登录即可访问。
    private boolean isPublicRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(method)
                || ("POST".equalsIgnoreCase(method) && "/api/users/register".equals(path))
                || ("POST".equalsIgnoreCase(method) && "/api/users/login".equals(path))
                || ("GET".equalsIgnoreCase(method) && "/api/health".equals(path));
    }
}
