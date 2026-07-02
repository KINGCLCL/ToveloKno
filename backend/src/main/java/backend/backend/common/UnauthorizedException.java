package backend.backend.common;

/**
 * 未登录或登录状态无效异常。
 *
 * token 缺失、格式错误、过期、账号不存在等场景都可以抛出这个异常。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
