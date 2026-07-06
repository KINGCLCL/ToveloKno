package backend.backend.learningresource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_resource")
public class LearningResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 40)
    private String type;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "original_filename", nullable = false, length = 260)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 260)
    private String storedFilename;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(length = 80)
    private String source;

    @Column(length = 800)
    private String description;

    @Column(nullable = false)
    private Boolean favorite = false;

    @Column(name = "progress_percent", nullable = false)
    private Integer progressPercent = 0;

    @Column(name = "current_page", nullable = false)
    private Integer currentPage = 1;

    @Column(name = "total_pages", nullable = false)
    private Integer totalPages = 0;

    @Column(name = "learned_minutes", nullable = false)
    private Integer learnedMinutes = 0;

    @Column(name = "annotation_count", nullable = false)
    private Integer annotationCount = 0;

    @Column(name = "annotations_json", columnDefinition = "TEXT")
    private String annotationsJson;

    @Column(name = "last_studied_at")
    private LocalDateTime lastStudiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getLearnedMinutes() {
        return learnedMinutes;
    }

    public void setLearnedMinutes(Integer learnedMinutes) {
        this.learnedMinutes = learnedMinutes;
    }

    public Integer getAnnotationCount() {
        return annotationCount;
    }

    public void setAnnotationCount(Integer annotationCount) {
        this.annotationCount = annotationCount;
    }

    public String getAnnotationsJson() {
        return annotationsJson;
    }

    public void setAnnotationsJson(String annotationsJson) {
        this.annotationsJson = annotationsJson;
    }

    public LocalDateTime getLastStudiedAt() {
        return lastStudiedAt;
    }

    public void setLastStudiedAt(LocalDateTime lastStudiedAt) {
        this.lastStudiedAt = lastStudiedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
