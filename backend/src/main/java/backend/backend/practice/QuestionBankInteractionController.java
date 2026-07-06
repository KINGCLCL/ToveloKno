package backend.backend.practice;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/question-bank")
public class QuestionBankInteractionController {

    private final QuestionBankInteractionService interactionService;

    public QuestionBankInteractionController(QuestionBankInteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping("/practice/questions")
    public ApiResponse<List<QuestionBankDtos.PracticeQuestion>> generatePractice(
            @Valid @RequestBody QuestionBankDtos.PracticeRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("练习生成成功", interactionService.generatePractice(request, currentUser));
    }

    @PostMapping("/practice/answers")
    public ApiResponse<QuestionBankDtos.AnswerResult> submitAnswer(
            @Valid @RequestBody QuestionBankDtos.AnswerRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("判题完成", interactionService.submitAnswer(request, currentUser));
    }

    @GetMapping("/dashboard")
    public ApiResponse<QuestionBankDtos.Dashboard> getDashboard(
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", interactionService.getDashboard(currentUser));
    }

    @GetMapping("/analytics")
    public ApiResponse<QuestionBankDtos.Analytics> getAnalytics(
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", interactionService.getAnalytics(currentUser));
    }

    @GetMapping("/settings")
    public ApiResponse<QuestionBankDtos.SettingsResponse> getSettings(
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", interactionService.getSettings(currentUser));
    }

    @PutMapping("/settings")
    public ApiResponse<QuestionBankDtos.SettingsResponse> updateSettings(
            @Valid @RequestBody QuestionBankDtos.SettingsRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("设置保存成功", interactionService.updateSettings(request, currentUser));
    }
}
