package backend.backend.controller;

import backend.backend.common.ApiResponse;
import backend.backend.dto.LoginRequest;
import backend.backend.dto.RegisterRequest;
import backend.backend.dto.UserResponse;
import backend.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口控制器。
 *
 * Controller 是前端访问后端的入口，这里定义注册、登录等 HTTP 接口。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 注册接口：前端 POST /api/users/register，并提交 username、password、email。
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", userService.register(request));
    }

    // 登录接口：前端 POST /api/users/login，并提交 username、password。
    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", userService.login(request));
    }

    // 处理业务异常，例如用户名重复、密码错误、账号禁用。
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleBusinessError(IllegalArgumentException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    // 处理参数校验异常，例如用户名为空、密码长度不够。
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ApiResponse.fail(message);
    }
}
