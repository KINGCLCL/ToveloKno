package backend.backend.studylink;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record StudyLinkOverviewResponse(
        long resourceCount,
        long questionCount,
        long resourceQuestionCount,
        long activeWrongCount,
        long pendingPlanCount,
        List<RecentQuestion> recentResourceQuestions,
        List<RecentPlan> recentPlans) {

    public record RecentQuestion(
            Long id,
            String content,
            String sourceResourceName,
            Integer sourcePage,
            LocalDateTime updatedAt) {
    }

    public record RecentPlan(
            Long id,
            String title,
            String targetType,
            Long targetId,
            String targetTitle,
            LocalDate planDate,
            String status) {
    }
}
