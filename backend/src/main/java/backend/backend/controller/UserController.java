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

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", userService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUserProfile(@CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", userService.getCurrentUserProfile(currentUser));
    }

    @PutMapping("/me/profile")
    public ApiResponse<UserResponse> updateCurrentUserProfile(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success("修改成功", userService.updateCurrentUserProfile(currentUser, request));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changeCurrentUserPassword(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changeCurrentUserPassword(currentUser, request);
        return ApiResponse.success("密码修改成功", null);
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserProfile(
            @PathVariable Long userId,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", userService.getUserProfile(userId, currentUser));
    }

    @PutMapping("/{userId}/profile")
    public ApiResponse<UserResponse> updateProfile(
            @PathVariable Long userId,
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success("修改成功", userService.updateProfile(userId, request, currentUser));
    }

    @PutMapping("/{userId}/password")
    public ApiResponse<Void> changePassword(
            @PathVariable Long userId,
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request, currentUser);
        return ApiResponse.success("密码修改成功", null);
    }
}
