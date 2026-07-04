package backend.backend.studyplan.controller;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import backend.backend.common.PageResponse;
import backend.backend.studyplan.dto.StudyPlanRequest;
import backend.backend.studyplan.dto.StudyPlanResponse;
import backend.backend.studyplan.dto.UpdateStatusRequest;
import backend.backend.studyplan.service.StudyPlanService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 学习计划模块接口。
 *
 * 提供学习计划的新增、查询、编辑、删除和完成状态管理功能。
 *
 * GET    /api/study-plans              分页查询计划列表
 * GET    /api/study-plans/{id}         查询单条计划详情
 * POST   /api/study-plans              新增计划
 * PUT    /api/study-plans/{id}         编辑计划
 * DELETE /api/study-plans/{id}         删除计划
 * PUT    /api/study-plans/{id}/status  修改计划状态
 */
@RestController
@RequestMapping("/api/study-plans")
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    public StudyPlanController(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
    }

    /**
     * 分页查询学习计划列表。
     *
     * /api/study-plans                         查询全部
     * /api/study-plans?status=pending           只查待完成
     * /api/study-plans?status=completed         只查已完成
     * /api/study-plans?planDate=2026-07-04      按日期筛选
     * /api/study-plans?page=1&size=10           分页参数
     */
    @GetMapping
    public ApiResponse<PageResponse<StudyPlanResponse>> listStudyPlans(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(
                "查询成功",
                studyPlanService.listStudyPlans(currentUser, status, planDate, page, size)
        );
    }

    /**
     * 查询单条学习计划详情。
     */
    @GetMapping("/{planId}")
    public ApiResponse<StudyPlanResponse> getStudyPlan(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long planId) {
        return ApiResponse.success(
                "查询成功",
                studyPlanService.getStudyPlan(planId, currentUser)
        );
    }

    /**
     * 新增学习计划。
     */
    @PostMapping
    public ApiResponse<StudyPlanResponse> createStudyPlan(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody StudyPlanRequest request) {
        return ApiResponse.success(
                "新增成功",
                studyPlanService.createStudyPlan(request, currentUser)
        );
    }

    /**
     * 编辑学习计划。
     */
    @PutMapping("/{planId}")
    public ApiResponse<StudyPlanResponse> updateStudyPlan(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long planId,
            @Valid @RequestBody StudyPlanRequest request) {
        return ApiResponse.success(
                "编辑成功",
                studyPlanService.updateStudyPlan(planId, request, currentUser)
        );
    }

    /**
     * 删除学习计划。
     */
    @DeleteMapping("/{planId}")
    public ApiResponse<Void> deleteStudyPlan(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long planId) {
        studyPlanService.deleteStudyPlan(planId, currentUser);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 修改学习计划状态。
     *
     * 将计划标记为 completed（已完成）、cancelled（已取消）或 pending（待完成）。
     */
    @PutMapping("/{planId}/status")
    public ApiResponse<StudyPlanResponse> updateStatus(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long planId,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ApiResponse.success(
                "状态更新成功",
                studyPlanService.updateStatus(planId, request.getStatus(), currentUser)
        );
    }
}
