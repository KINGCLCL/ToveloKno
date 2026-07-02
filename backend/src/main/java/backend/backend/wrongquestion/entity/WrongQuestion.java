package backend.backend.wrongquestion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 错题本实体类，对应数据库 wrong_question 表。
 */
@Entity
@Table(name = "wrong_question")
public class WrongQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "wrong_count", nullable = false)
    private Integer wrongCount = 1;

    /**
     * 0 表示仍在错题本中复习，1 表示已经掌握。
     */
    @Column(nullable = false)
    private Integer mastered = 0;

    @Column(name = "last_wrong_at", nullable = false)
    private LocalDateTime lastWrongAt;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public Integer getWrongCount() {
        return wrongCount;
    }

    public Integer getMastered() {
        return mastered;
    }

    public LocalDateTime getLastWrongAt() {
        return lastWrongAt;
    }

    public LocalDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public void setWrongCount(Integer wrongCount) {
        this.wrongCount = wrongCount;
    }

    public void setMastered(Integer mastered) {
        this.mastered = mastered;
    }

    public void setLastWrongAt(LocalDateTime lastWrongAt) {
        this.lastWrongAt = lastWrongAt;
    }

    public void setLastReviewedAt(LocalDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }
}