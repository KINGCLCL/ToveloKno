package backend.backend.question;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import backend.backend.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目管理接口。
 *
 * 所有接口都需要登录，且只操作当前用户创建的题目。
 */
@Validated
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * 分页查询题目，支持前端题目管理页的全部筛选条件。
     */
    @GetMapping
    public ApiResponse<PageResponse<QuestionResponse>> listQuestions(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页数量不能小于1")
            @Max(value = 100, message = "每页最多100条") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String knowledgePoint,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) QuestionType questionType,
            @RequestParam(required = false) @Min(value = 1, message = "难度不能小于1")
            @Max(value = 5, message = "难度不能大于5") Integer difficulty,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(defaultValue = "false") boolean deleted,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.success(
                "查询成功",
                questionService.listQuestions(
                        currentUser,
                        page,
                        size,
                        keyword,
                        subject,
                        knowledgePoint,
                        categoryId,
                        questionType,
                        difficulty,
                        status,
                        deleted,
                        sort,
                        direction));
    }

    @GetMapping("/stats")
    public ApiResponse<QuestionStatsResponse> getStats(@CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", questionService.getStats(currentUser));
    }

    @GetMapping("/{questionId}")
    public ApiResponse<QuestionResponse> getQuestion(
            @PathVariable Long questionId,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", questionService.getQuestion(questionId, currentUser));
    }

    @PostMapping
    public ApiResponse<QuestionResponse> createQuestion(
            @Valid @RequestBody QuestionSaveRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("新增成功", questionService.createQuestion(request, currentUser));
    }

    @PutMapping("/{questionId}")
    public ApiResponse<QuestionResponse> updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionSaveRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("修改成功", questionService.updateQuestion(questionId, request, currentUser));
    }

    /**
     * 移入回收站，不会立即物理删除。
     */
    @DeleteMapping("/{questionId}")
    public ApiResponse<Void> moveToRecycleBin(
            @PathVariable Long questionId,
            @CurrentUser AuthenticatedUser currentUser) {
        questionService.moveToRecycleBin(questionId, currentUser);
        return ApiResponse.success("已移入回收站", null);
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Integer> batchMoveToRecycleBin(
            @Valid @RequestBody QuestionBatchRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success(
                "批量删除成功",
                questionService.batchMoveToRecycleBin(request, currentUser));
    }

    @PutMapping("/batch/status")
    public ApiResponse<Integer> batchUpdateStatus(
            @Valid @RequestBody QuestionBatchStatusRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success(
                "状态修改成功",
                questionService.batchUpdateStatus(request, currentUser));
    }

    @PutMapping("/{questionId}/restore")
    public ApiResponse<QuestionResponse> restoreQuestion(
            @PathVariable Long questionId,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("恢复成功", questionService.restoreQuestion(questionId, currentUser));
    }

    @DeleteMapping("/recycle-bin/{questionId}")
    public ApiResponse<Void> permanentlyDelete(
            @PathVariable Long questionId,
            @CurrentUser AuthenticatedUser currentUser) {
        questionService.permanentlyDelete(questionId, currentUser);
        return ApiResponse.success("永久删除成功", null);
    }
}
