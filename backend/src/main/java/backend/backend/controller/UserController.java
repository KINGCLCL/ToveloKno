package backend.backend.controller;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import backend.backend.dto.ChangePasswordRequest;
import backend.backend.dto.LoginRequest;
import backend.backend.dto.LoginResponse;
import backend.backend.dto.RegisterRequest;
import backend.backend.dto.UpdateUserProfileRequest;
import backend.backend.dto.UserResponse;
import backend.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户模块接口入口。
 *
 * 注册、登录是公开接口；/me 和 /{userId} 相关接口需要登录态。
 * 后续页面开发优先使用 /me 系列接口，减少前端传错 userId 的风险。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册。
     *
     * 注册成功只返回用户基本信息，不自动登录；前端可以继续调用登录接口拿 token。
     */
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", userService.register(request));
    }

    /**
     * 用户登录。
     *
     * 登录成功返回 token 和用户信息，前端会把 token 放到后续请求头里。
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", userService.login(request));
    }

    /**
     * 查询当前登录用户资料。
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUserProfile(@CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", userService.getCurrentUserProfile(currentUser));
    }

    /**
     * 修改当前登录用户资料。
     */
    @PutMapping("/me/profile")
    public ApiResponse<UserResponse> updateCurrentUserProfile(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success("修改成功", userService.updateCurrentUserProfile(currentUser, request));
    }

    /**
     * 修改当前登录用户密码。
     */
    @PutMapping("/me/password")
    public ApiResponse<Void> changeCurrentUserPassword(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changeCurrentUserPassword(currentUser, request);
        return ApiResponse.success("密码修改成功", null);
    }

    /**
     * 按用户 id 查询资料。
     *
     * 保留这个接口方便管理员或调试使用；普通用户只能查自己。
     */
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserProfile(
            @PathVariable Long userId,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", userService.getUserProfile(userId, currentUser));
    }

    /**
     * 按用户 id 修改资料。
     *
     * Service 层会校验“本人或管理员”权限。
     */
    @PutMapping("/{userId}/profile")
    public ApiResponse<UserResponse> updateProfile(
            @PathVariable Long userId,
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success("修改成功", userService.updateProfile(userId, request, currentUser));
    }

    /**
     * 按用户 id 修改密码。
     *
     * 当前实现仍要求旧密码校验通过，避免管理员误改造成安全风险。
     */
    @PutMapping("/{userId}/password")
    public ApiResponse<Void> changePassword(
            @PathVariable Long userId,
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request, currentUser);
        return ApiResponse.success("密码修改成功", null);
    }
}
