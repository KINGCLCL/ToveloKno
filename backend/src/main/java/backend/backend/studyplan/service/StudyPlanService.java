package backend.backend.studyplan.service;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.common.PageResponse;
import backend.backend.service.OperationLogService;
import backend.backend.studyplan.dto.StudyPlanRequest;
import backend.backend.studyplan.dto.StudyPlanResponse;
import backend.backend.studyplan.entity.StudyPlan;
import backend.backend.studyplan.repository.StudyPlanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习计划业务层。
 */
@Service
public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final OperationLogService operationLogService;

    public StudyPlanService(
            StudyPlanRepository studyPlanRepository,
            OperationLogService operationLogService) {
        this.studyPlanRepository = studyPlanRepository;
        this.operationLogService = operationLogService;
    }

    /**
     * 分页查询当前用户的学习计划。
     *
     * @param status   可选，按状态筛选（pending/completed/cancelled）
     * @param planDate 可选，按日期筛选
     * @param page     页码，从 1 开始
     * @param size     每页条数
     */
    public PageResponse<StudyPlanResponse> listStudyPlans(
            AuthenticatedUser currentUser,
            String status,
            LocalDate planDate,
            int page,
            int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<StudyPlan> planPage = studyPlanRepository.findStudyPlanPage(
                currentUser.getId(), status, planDate, pageRequest);

        Page<StudyPlanResponse> responsePage = planPage.map(this::toResponse);
        return PageResponse.from(responsePage);
    }

    /**
     * 查询单条学习计划详情。
     */
    public StudyPlanResponse getStudyPlan(Long planId, AuthenticatedUser currentUser) {
        StudyPlan plan = findOwnStudyPlan(planId, currentUser.getId());
        return toResponse(plan);
    }

    /**
     * 新增学习计划。
     */
    @Transactional
    public StudyPlanResponse createStudyPlan(StudyPlanRequest request, AuthenticatedUser currentUser) {
        StudyPlan plan = new StudyPlan();
        plan.setUserId(currentUser.getId());
        plan.setTitle(request.getTitle());
        plan.setContent(request.getContent());
        plan.setPlanDate(request.getPlanDate());
        applyTarget(plan, request);
        plan.setStatus("pending");
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());

        StudyPlan saved = studyPlanRepository.save(plan);

        operationLogService.record(
                currentUser.getId(),
                "STUDY_PLAN_CREATE",
                "新增学习计划：" + saved.getId()
        );

        return toResponse(saved);
    }

    /**
     * 编辑学习计划。
     */
    @Transactional
    public StudyPlanResponse updateStudyPlan(Long planId, StudyPlanRequest request, AuthenticatedUser currentUser) {
        StudyPlan plan = findOwnStudyPlan(planId, currentUser.getId());

        plan.setTitle(request.getTitle());
        plan.setContent(request.getContent());
        plan.setPlanDate(request.getPlanDate());
        applyTarget(plan, request);
        plan.setUpdatedAt(LocalDateTime.now());

        StudyPlan saved = studyPlanRepository.save(plan);

        operationLogService.record(
                currentUser.getId(),
                "STUDY_PLAN_UPDATE",
                "编辑学习计划：" + planId
        );

        return toResponse(saved);
    }

    /**
     * 删除学习计划。
     */
    @Transactional
    public void deleteStudyPlan(Long planId, AuthenticatedUser currentUser) {
        StudyPlan plan = findOwnStudyPlan(planId, currentUser.getId());
        studyPlanRepository.delete(plan);

        operationLogService.record(
                currentUser.getId(),
                "STUDY_PLAN_DELETE",
                "删除学习计划：" + planId
        );
    }

    /**
     * 修改学习计划状态（完成 / 取消 / 恢复待完成）。
     */
    @Transactional
    public StudyPlanResponse updateStatus(Long planId, String newStatus, AuthenticatedUser currentUser) {
        StudyPlan plan = findOwnStudyPlan(planId, currentUser.getId());

        plan.setStatus(newStatus);
        plan.setUpdatedAt(LocalDateTime.now());

        if ("completed".equals(newStatus)) {
            plan.setCompletedAt(LocalDateTime.now());
        } else {
            plan.setCompletedAt(null);
        }

        StudyPlan saved = studyPlanRepository.save(plan);

        operationLogService.record(
                currentUser.getId(),
                "STUDY_PLAN_STATUS_" + newStatus.toUpperCase(),
                "修改计划状态为 " + newStatus + "：" + planId
        );

        return toResponse(saved);
    }

    /**
     * 查找属于当前用户的学习计划，不存在则抛出异常。
     */
    private StudyPlan findOwnStudyPlan(Long planId, Long userId) {
        return studyPlanRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new IllegalArgumentException("学习计划不存在或无权操作"));
    }

    /**
     * 实体转响应对象。
     */
    private StudyPlanResponse toResponse(StudyPlan plan) {
        return new StudyPlanResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getContent(),
                plan.getPlanDate(),
                plan.getTargetType(),
                plan.getTargetId(),
                plan.getTargetTitle(),
                plan.getStatus(),
                plan.getCompletedAt(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }

    private void applyTarget(StudyPlan plan, StudyPlanRequest request) {
        plan.setTargetType(normalizeText(request.getTargetType(), 40));
        plan.setTargetId(request.getTargetId());
        plan.setTargetTitle(normalizeText(request.getTargetTitle(), 180));
    }

    private String normalizeText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
