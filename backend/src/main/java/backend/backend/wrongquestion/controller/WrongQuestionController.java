package backend.backend.wrongquestion.controller;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import backend.backend.wrongquestion.dto.WrongQuestionResponse;
import backend.backend.wrongquestion.service.WrongQuestionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 错题本模块接口。
 */
@RestController
@RequestMapping("/api/wrong-questions")
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    public WrongQuestionController(WrongQuestionService wrongQuestionService) {
        this.wrongQuestionService = wrongQuestionService;
    }

    /**
     * 查询错题列表。
     *
     * /api/wrong-questions              查询全部错题
     * /api/wrong-questions?mastered=false 查询未掌握错题
     * /api/wrong-questions?mastered=true  查询已掌握错题
     */
    @GetMapping
    public ApiResponse<List<WrongQuestionResponse>> listWrongQuestions(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam(required = false) Boolean mastered) {
        return ApiResponse.success(
                "查询成功",
                wrongQuestionService.listWrongQuestions(currentUser, mastered)
        );
    }

    /**
     * 标记掌握。
     */
    @PutMapping("/{wrongQuestionId}/mastered")
    public ApiResponse<Void> markAsMastered(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long wrongQuestionId) {
        wrongQuestionService.markAsMastered(wrongQuestionId, currentUser);
        return ApiResponse.success("已标记为掌握", null);
    }

    /**
     * 移出错题本。
     */
    @DeleteMapping("/{wrongQuestionId}")
    public ApiResponse<Void> removeWrongQuestion(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long wrongQuestionId) {
        wrongQuestionService.removeWrongQuestion(wrongQuestionId, currentUser);
        return ApiResponse.success("已移出错题本", null);
    }
}