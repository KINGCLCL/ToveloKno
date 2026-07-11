package backend.backend.admin;

import backend.backend.admin.AdminDtos.AdminOverviewResponse;
import backend.backend.admin.AdminDtos.AdminUserResponse;
import backend.backend.admin.AdminDtos.UpdateUserStatusRequest;
import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewResponse> overview(@CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", adminService.overview(currentUser));
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserResponse>> users(@CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", adminService.users(currentUser));
    }

    @PutMapping("/users/{userId}/status")
    public ApiResponse<AdminUserResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("账号状态已更新", adminService.updateUserStatus(userId, request.status(), currentUser));
    }
}
