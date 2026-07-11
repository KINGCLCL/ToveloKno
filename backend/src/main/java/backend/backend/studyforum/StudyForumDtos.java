package backend.backend.studyforum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class StudyForumDtos {
    private StudyForumDtos() {
    }

    public record ForumThreadRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 40) String boardId,
            @NotBlank @Size(max = 1200) String content,
            @Size(max = 300) String tags) {
    }

    public record ForumReplyRequest(
            @NotBlank @Size(max = 800) String content) {
    }

    public record ForumReplyResponse(
            Long id,
            Long userId,
            String author,
            String content,
            LocalDateTime createdAt) {
    }

    public record ForumThreadResponse(
            Long id,
            Long userId,
            String author,
            String boardId,
            String title,
            String content,
            List<String> tags,
            boolean pinned,
            int views,
            int likes,
            int replyCount,
            boolean favorite,
            boolean liked,
            String lastReplyAuthor,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime lastRepliedAt,
            List<ForumReplyResponse> replies) {
    }
}
