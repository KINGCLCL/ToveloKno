package backend.backend.studyforum;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import backend.backend.studyforum.StudyForumDtos.ForumReplyRequest;
import backend.backend.studyforum.StudyForumDtos.ForumThreadRequest;
import backend.backend.studyforum.StudyForumDtos.ForumThreadResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
public class StudyForumController {
    private final StudyForumService studyForumService;

    public StudyForumController(StudyForumService studyForumService) {
        this.studyForumService = studyForumService;
    }

    @GetMapping("/threads")
    public ApiResponse<List<ForumThreadResponse>> list(@CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", studyForumService.list(currentUser));
    }

    @PostMapping("/threads")
    public ApiResponse<ForumThreadResponse> createThread(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody ForumThreadRequest request) {
        return ApiResponse.success("发布成功", studyForumService.createThread(currentUser, request));
    }

    @PutMapping("/threads/{threadId}/view")
    public ApiResponse<ForumThreadResponse> viewThread(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long threadId) {
        return ApiResponse.success("已记录浏览", studyForumService.viewThread(threadId, currentUser));
    }

    @PostMapping("/threads/{threadId}/replies")
    public ApiResponse<ForumThreadResponse> addReply(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long threadId,
            @Valid @RequestBody ForumReplyRequest request) {
        return ApiResponse.success("回复成功", studyForumService.addReply(threadId, currentUser, request));
    }

    @PutMapping("/threads/{threadId}/favorite")
    public ApiResponse<ForumThreadResponse> toggleFavorite(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long threadId) {
        return ApiResponse.success("收藏已更新", studyForumService.toggleFavorite(threadId, currentUser));
    }

    @PutMapping("/threads/{threadId}/like")
    public ApiResponse<ForumThreadResponse> toggleLike(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long threadId) {
        return ApiResponse.success("点赞已更新", studyForumService.toggleLike(threadId, currentUser));
    }
}
