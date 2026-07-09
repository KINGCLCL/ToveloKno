package backend.backend.aiassistant;

import backend.backend.question.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class AiAssistantDtos {
    private AiAssistantDtos() {
    }

    public record AiAssistantStatusResponse(
            boolean configured,
            String provider,
            String model) {
    }

    public record AiAnalysisRequest(
            Long resourceId,
            @Size(max = 20000) String text,
            @Size(max = 80) String subject,
            @Size(max = 80) String knowledgePoint,
            @Min(1) @Max(20) Integer questionLimit,
            @Min(1) @Max(14) Integer planDays,
            LocalDate planStartDate) {
    }

    public record AiAnalysisResponse(
            boolean configured,
            String provider,
            String model,
            String sourceTitle,
            String sourceExcerpt,
            List<AiQuestionDraft> questions,
            List<AiKnowledgePoint> knowledgePoints,
            List<AiPlanDraft> plans,
            List<String> warnings) {
    }

    public record AiQuestionDraft(
            String content,
            QuestionType questionType,
            List<String> options,
            String correctAnswer,
            String analysis,
            Integer difficulty,
            String subject,
            String knowledgePoint,
            String sourceExcerpt) {
    }

    public record AiKnowledgePoint(
            String title,
            String summary,
            List<String> keyItems,
            List<String> pitfalls,
            Integer priority) {
    }

    public record AiPlanDraft(
            String title,
            String content,
            LocalDate planDate,
            String targetType,
            String targetTitle) {
    }
}
