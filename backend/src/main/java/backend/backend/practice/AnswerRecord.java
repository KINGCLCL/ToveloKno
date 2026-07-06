package backend.backend.practice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "answer_record")
public class AnswerRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "user_answer", nullable = false, length = 1000)
    private String userAnswer;

    @Column(name = "is_correct", nullable = false)
    private Boolean correct = false;

    @Column(name = "practice_mode", nullable = false, length = 30)
    private String practiceMode = "free";

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    @PrePersist
    public void prePersist() {
        if (answeredAt == null) answeredAt = LocalDateTime.now();
        if (correct == null) correct = false;
        if (practiceMode == null || practiceMode.isBlank()) practiceMode = "free";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }
    public String getPracticeMode() { return practiceMode; }
    public void setPracticeMode(String practiceMode) { this.practiceMode = practiceMode; }
    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
}
