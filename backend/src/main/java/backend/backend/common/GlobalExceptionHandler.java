package backend.backend.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * 业务代码只需要抛出合适的异常，这里统一转换成 ApiResponse，
 * 保证前端收到的错误格式稳定，同时保留正确的 HTTP 状态码。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 @Valid 校验失败，例如 DTO 字段为空、长度不符合要求。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().isEmpty()
                ? "请求参数不正确"
                : exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return fail(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 处理路径参数、请求参数上的校验失败。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return fail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /**
     * 处理普通业务错误，例如重复数据、旧密码错误、资源不存在。
     */
    @ExceptionHandler({BusinessException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleBusinessError(RuntimeException exception) {
        return fail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /**
     * 处理未登录或登录状态失效。
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException exception) {
        return fail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    /**
     * 处理已登录但权限不足。
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException exception) {
        return fail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    /**
     * 处理系统状态异常，例如基础数据缺失。
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleSystemStateError(IllegalStateException exception) {
        return fail(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    /**
     * 兜底处理未知异常。
     *
     * 不把详细堆栈返回给前端，避免泄露服务端实现细节。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownError(Exception exception) {
        return fail(HttpStatus.INTERNAL_SERVER_ERROR, "服务器开小差了，请稍后再试");
    }

    // 统一构造失败响应，减少每个异常方法里的重复代码。
    private ResponseEntity<ApiResponse<Void>> fail(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(message));
    }
}
