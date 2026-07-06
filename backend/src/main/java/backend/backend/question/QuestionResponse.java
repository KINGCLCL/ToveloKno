package backend.backend.question;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目详情响应。
 */
public class QuestionResponse {

    private final Long id;
    private final String content;
    private final QuestionType questionType;
    private final List<String> options;
    private final String correctAnswer;
    private final String analysis;
    private final Integer difficulty;
    private final String subject;
    private final String knowledgePoint;
    private final QuestionStatus status;
    private final Long categoryId;
    private final Long createdBy;
    private final boolean deleted;
    private final LocalDateTime deletedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public QuestionResponse(
            Long id,
            String content,
            QuestionType questionType,
            List<String> options,
            String correctAnswer,
            String analysis,
            Integer difficulty,
            String subject,
            String knowledgePoint,
            QuestionStatus status,
            Long categoryId,
            Long createdBy,
            boolean deleted,
            LocalDateTime deletedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.content = content;
        this.questionType = questionType;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.analysis = analysis;
        this.difficulty = difficulty;
        this.subject = subject;
        this.knowledgePoint = knowledgePoint;
        this.status = status;
        this.categoryId = categoryId;
        this.createdBy = createdBy;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getAnalysis() {
        return analysis;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public String getSubject() {
        return subject;
    }

    public String getKnowledgePoint() {
        return knowledgePoint;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
