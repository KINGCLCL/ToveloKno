package backend.backend.wrongquestion.dto;

import java.time.LocalDateTime;

/**
 * 错题本列表返回对象。
 */
public class WrongQuestionResponse {

    private Long id;
    private Long questionId;
    private String content;
    private String questionType;
    private String correctAnswer;
    private String analysis;
    private Integer difficulty;
    private String sourceType;
    private Long sourceResourceId;
    private String sourceResourceName;
    private Integer sourcePage;
    private String sourceExcerpt;
    private Integer wrongCount;
    private Boolean mastered;
    private LocalDateTime lastWrongAt;
    private LocalDateTime lastReviewedAt;

    public WrongQuestionResponse(
            Long id,
            Long questionId,
            String content,
            String questionType,
            String correctAnswer,
            String analysis,
            Integer difficulty,
            String sourceType,
            Long sourceResourceId,
            String sourceResourceName,
            Integer sourcePage,
            String sourceExcerpt,
            Integer wrongCount,
            Boolean mastered,
            LocalDateTime lastWrongAt,
            LocalDateTime lastReviewedAt) {
        this.id = id;
        this.questionId = questionId;
        this.content = content;
        this.questionType = questionType;
        this.correctAnswer = correctAnswer;
        this.analysis = analysis;
        this.difficulty = difficulty;
        this.sourceType = sourceType;
        this.sourceResourceId = sourceResourceId;
        this.sourceResourceName = sourceResourceName;
        this.sourcePage = sourcePage;
        this.sourceExcerpt = sourceExcerpt;
        this.wrongCount = wrongCount;
        this.mastered = mastered;
        this.lastWrongAt = lastWrongAt;
        this.lastReviewedAt = lastReviewedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public String getContent() {
        return content;
    }

    public String getQuestionType() {
        return questionType;
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

    public String getSourceType() {
        return sourceType;
    }

    public Long getSourceResourceId() {
        return sourceResourceId;
    }

    public String getSourceResourceName() {
        return sourceResourceName;
    }

    public Integer getSourcePage() {
        return sourcePage;
    }

    public String getSourceExcerpt() {
        return sourceExcerpt;
    }

    public Integer getWrongCount() {
        return wrongCount;
    }

    public Boolean getMastered() {
        return mastered;
    }

    public LocalDateTime getLastWrongAt() {
        return lastWrongAt;
    }

    public LocalDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }
}
