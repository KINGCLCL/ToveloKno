package backend.backend.learningresource;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import backend.backend.question.QuestionStatus;
import backend.backend.question.QuestionType;

import java.time.LocalDateTime;
import java.util.List;

public final class LearningResourceDtos {
    private LearningResourceDtos() {
    }

    public record ResourceResponse(
            Long id,
            String name,
            String type,
            String mimeType,
            String originalFilename,
            String fileUrl,
            Long fileSize,
            String sizeText,
            String source,
            String description,
            boolean favorite,
            int progressPercent,
            int currentPage,
            int totalPages,
            int learnedMinutes,
            int annotationCount,
            List<AnnotationPayload> annotations,
            LocalDateTime lastStudiedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record ProgressRequest(
            @NotNull @Min(0) @Max(100) Integer progressPercent,
            @NotNull @Min(1) Integer currentPage,
            @NotNull @Min(0) Integer totalPages,
            @NotNull @Min(0) Integer learnedMinutes) {
    }

    public record AnnotationsRequest(
            @NotNull List<AnnotationPayload> annotations) {
    }

    public record ResourceQuestionRequest(
            @NotBlank @Size(max = 10000) String content,
            @NotNull QuestionType questionType,
            @Size(max = 8) List<@NotBlank @Size(max = 500) String> options,
            @NotBlank @Size(max = 1000) String correctAnswer,
            @Size(max = 10000) String analysis,
            @NotNull @Min(1) @Max(5) Integer difficulty,
            @Size(max = 80) String subject,
            @Size(max = 80) String knowledgePoint,
            QuestionStatus status,
            Long categoryId,
            @Min(1) Integer sourcePage,
            @Size(max = 4000) String sourceExcerpt) {
    }

    public record AnnotationPayload(
            String id,
            @Size(max = 40) String type,
            @Size(max = 40) String color,
            Integer page,
            Double x,
            Double y,
            Double width,
            Double height,
            List<PointPayload> points,
            @Size(max = 1200) String text,
            LocalDateTime createdAt) {
    }

    public record PointPayload(
            Double x,
            Double y) {
    }
}
