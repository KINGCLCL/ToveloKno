package backend.backend.common;

/**
 * 无权限异常。
 *
 * 已登录但不能访问某个资源时使用，例如普通用户操作别人的数据。
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
