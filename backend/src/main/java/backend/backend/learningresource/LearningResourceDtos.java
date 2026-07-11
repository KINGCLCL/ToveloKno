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

    public record QuestionExtractionRequest(
            @Min(1) @Max(80) Integer limit,
            QuestionStatus status) {
    }

    public record ResourceImageQuestionRequest(
            @Size(max = 120) String title,
            @Size(max = 2_000_000) String imageDataUrl,
            @Size(max = 500) String imageUrl,
            @Size(max = 1000) String correctAnswer,
            Long categoryId,
            @Min(1) Integer sourcePage,
            QuestionStatus status) {
    }

    public record QuestionExtractionResponse(
            Long resourceId,
            String resourceName,
            int createdCount,
            int detectedCount,
            List<String> warnings,
            List<ExtractedQuestionPreview> previews,
            List<backend.backend.question.QuestionResponse> questions) {
    }

    public record ExtractedQuestionPreview(
            Long questionId,
            String content,
            QuestionType questionType,
            List<String> options,
            String correctAnswer,
            String analysis,
            Integer difficulty,
            String subject,
            String knowledgePoint,
            Integer sourcePage,
            String sourceExcerpt) {
    }

    public record AnnotationPayload(
            String id,
            @Size(max = 40) String type,
            @Size(max = 40) String color,
            @Size(max = 40) String coordinateSpace,
            Integer page,
            Double pageWidth,
            Double pageHeight,
            Double zoom,
            Double x,
            Double y,
            Double width,
            Double height,
            List<PointPayload> points,
            @Size(max = 1200) String text,
            String createdAt) {
    }

    public record PointPayload(
            Double x,
            Double y) {
    }
}
