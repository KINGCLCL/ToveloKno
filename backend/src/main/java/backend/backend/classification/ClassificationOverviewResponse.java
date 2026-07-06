package backend.backend.classification;

import java.time.LocalDateTime;
import java.util.List;

public class ClassificationOverviewResponse {

    private final List<CategoryItem> categories;
    private final List<CategoryNode> categoryTree;
    private final List<KnowledgePointItem> knowledgePoints;
    private final List<TagItem> tags;
    private final List<DifficultyItem> difficulties;
    private final Summary summary;

    public ClassificationOverviewResponse(
            List<CategoryItem> categories,
            List<CategoryNode> categoryTree,
            List<KnowledgePointItem> knowledgePoints,
            List<TagItem> tags,
            List<DifficultyItem> difficulties,
            Summary summary) {
        this.categories = categories;
        this.categoryTree = categoryTree;
        this.knowledgePoints = knowledgePoints;
        this.tags = tags;
        this.difficulties = difficulties;
        this.summary = summary;
    }

    public List<CategoryItem> getCategories() {
        return categories;
    }

    public List<CategoryNode> getCategoryTree() {
        return categoryTree;
    }

    public List<KnowledgePointItem> getKnowledgePoints() {
        return knowledgePoints;
    }

    public List<TagItem> getTags() {
        return tags;
    }

    public List<DifficultyItem> getDifficulties() {
        return difficulties;
    }

    public Summary getSummary() {
        return summary;
    }

    public record CategoryItem(
            Long id,
            String name,
            String description,
            Long parentId,
            String parentName,
            boolean active,
            int sortOrder,
            long questionCount,
            LocalDateTime updatedAt) {
    }

    public record CategoryNode(
            Long id,
            String name,
            boolean active,
            long questionCount,
            long totalQuestionCount,
            List<CategoryNode> children) {
    }

    public record KnowledgePointItem(
            String name,
            String subject,
            long questionCount,
            long publishedCount,
            LocalDateTime updatedAt) {
    }

    public record TagItem(
            Long id,
            String name,
            String description,
            LocalDateTime updatedAt) {
    }

    public record DifficultyItem(
            int level,
            String label,
            long questionCount,
            double percentage) {
    }

    public record Summary(
            long categoryCount,
            long knowledgePointCount,
            long tagCount,
            long classifiedQuestionCount,
            long unclassifiedQuestionCount) {
    }
}
