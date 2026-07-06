package backend.backend.practice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "question_bank_setting")
public class QuestionBankSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName = "我的题库";

    @Column(length = 500)
    private String description;

    @Column(name = "member_edit", nullable = false)
    private Boolean memberEdit = true;

    @Column(name = "member_export", nullable = false)
    private Boolean memberExport = true;

    @Column(name = "review_required", nullable = false)
    private Boolean reviewRequired = false;

    @Column(name = "practice_count", nullable = false)
    private Integer practiceCount = 10;

    @Column(name = "default_difficulty", nullable = false, length = 20)
    private String defaultDifficulty = "中等";

    @Column(name = "sort_mode", nullable = false, length = 20)
    private String sortMode = "随机排序";

    @Column(name = "show_answer", nullable = false, length = 20)
    private String showAnswer = "立即显示";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getMemberEdit() { return memberEdit; }
    public void setMemberEdit(Boolean memberEdit) { this.memberEdit = memberEdit; }
    public Boolean getMemberExport() { return memberExport; }
    public void setMemberExport(Boolean memberExport) { this.memberExport = memberExport; }
    public Boolean getReviewRequired() { return reviewRequired; }
    public void setReviewRequired(Boolean reviewRequired) { this.reviewRequired = reviewRequired; }
    public Integer getPracticeCount() { return practiceCount; }
    public void setPracticeCount(Integer practiceCount) { this.practiceCount = practiceCount; }
    public String getDefaultDifficulty() { return defaultDifficulty; }
    public void setDefaultDifficulty(String defaultDifficulty) { this.defaultDifficulty = defaultDifficulty; }
    public String getSortMode() { return sortMode; }
    public void setSortMode(String sortMode) { this.sortMode = sortMode; }
    public String getShowAnswer() { return showAnswer; }
    public void setShowAnswer(String showAnswer) { this.showAnswer = showAnswer; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
