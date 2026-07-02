package backend.backend.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Controller 方法参数需要注入当前登录用户。
 *
 * 用法示例：
 * public ApiResponse<?> list(@CurrentUser AuthenticatedUser currentUser)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
