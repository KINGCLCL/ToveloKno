package backend.backend.admin;

import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record AdminOverviewResponse(
            long userCount,
            long activeUserCount,
            long resourceCount,
            long questionCount,
            long sharedResourceCount,
            long forumThreadCount,
            long studyPlanCount,
            List<AdminActivity> recentActivities) {
    }

    public record AdminUserResponse(
            Long id,
            String username,
            String nickname,
            String email,
            Integer status,
            List<String> roles,
            LocalDateTime createdAt) {
    }

    public record AdminActivity(
            String type,
            String title,
            String detail,
            LocalDateTime occurredAt) {
    }

    public record UpdateUserStatusRequest(Integer status) {
    }
}
