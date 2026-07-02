package backend.backend.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().isEmpty()
                ? "请求参数不正确"
                : exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return fail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return fail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({BusinessException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleBusinessError(RuntimeException exception) {
        return fail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException exception) {
        return fail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException exception) {
        return fail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleSystemStateError(IllegalStateException exception) {
        return fail(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownError(Exception exception) {
        return fail(HttpStatus.INTERNAL_SERVER_ERROR, "服务器开小差了，请稍后再试");
    }

    private ResponseEntity<ApiResponse<Void>> fail(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(message));
    }
}
