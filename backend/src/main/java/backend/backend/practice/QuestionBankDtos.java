package backend.backend.practice;

import backend.backend.question.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class QuestionBankDtos {
    private QuestionBankDtos() {
    }

    public record PracticeRequest(
            @NotBlank(message = "练习模式不能为空") String mode,
            @Min(value = 1, message = "练习题数不能小于1")
            @Max(value = 50, message = "练习题数不能超过50") Integer count,
            Long categoryId,
            @Size(max = 80, message = "知识点名称不能超过80个字符") String knowledgePoint) {
    }

    public record PracticeQuestion(
            Long id,
            String content,
            QuestionType questionType,
            List<String> options,
            Integer difficulty,
            String subject,
            String knowledgePoint,
            String correctAnswer,
            String analysis) {
    }

    public record AnswerRequest(
            @NotNull(message = "题目ID不能为空") Long questionId,
            @Size(max = 1000, message = "答案不能超过1000个字符") String userAnswer,
            @NotBlank(message = "练习模式不能为空") String mode,
            Boolean selfCorrect) {
        public AnswerRequest(Long questionId, String userAnswer, String mode) {
            this(questionId, userAnswer, mode, null);
        }
    }

    public record AnswerResult(
            boolean correct,
            String correctAnswer,
            String analysis,
            int wrongCount,
            boolean mastered) {
    }

    public record RecentAttempt(
            Long questionId,
            String title,
            String subject,
            boolean correct,
            String mode,
            LocalDateTime answeredAt) {
    }

    public record WeakPoint(String name, long attempts, long wrongCount, int accuracy) {
    }

    public record TodaySummary(
            long practiceCount,
            long wrongReviewCount,
            long activeWrongCount,
            long estimatedMinutes,
            int goal,
            int progress) {
    }

    public record Dashboard(
            int streakDays,
            List<RecentAttempt> recentAttempts,
            TodaySummary today,
            List<WeakPoint> weakPoints) {
    }

    public record TrendPoint(LocalDate date, long attempts, int accuracy) {
    }

    public record TypeAccuracy(QuestionType questionType, long attempts, int accuracy) {
    }

    public record AnalyticsSummary(
            long totalQuestions,
            long answeredQuestions,
            int accuracy,
            long activeWrongCount,
            long estimatedMinutes) {
    }

    public record Analytics(
            AnalyticsSummary summary,
            List<TrendPoint> trend,
            List<TypeAccuracy> typeAccuracy,
            List<WeakPoint> weakPoints) {
    }

    public record SettingsRequest(
            @NotBlank(message = "题库名称不能为空")
            @Size(max = 100, message = "题库名称不能超过100个字符") String bankName,
            @Size(max = 500, message = "题库描述不能超过500个字符") String description,
            @NotNull Boolean memberEdit,
            @NotNull Boolean memberExport,
            @NotNull Boolean reviewRequired,
            @NotNull @Min(1) @Max(50) Integer practiceCount,
            @NotBlank String difficulty,
            @NotBlank String sort,
            @NotBlank String showAnswer) {
    }

    public record SettingsResponse(
            String bankName,
            String description,
            boolean memberEdit,
            boolean memberExport,
            boolean reviewRequired,
            int practiceCount,
            String difficulty,
            String sort,
            String showAnswer,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
