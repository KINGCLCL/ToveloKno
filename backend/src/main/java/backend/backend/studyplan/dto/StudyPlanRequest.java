package backend.backend.studyplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 学习计划创建 / 更新请求体。
 */
public class StudyPlanRequest {

    @NotBlank(message = "计划标题不能为空")
    @Size(max = 150, message = "标题长度不能超过150个字符")
    private String title;

    @Size(max = 2000, message = "内容长度不能超过2000个字符")
    private String content;

    @NotNull(message = "计划日期不能为空")
    private LocalDate planDate;

    @Size(max = 40)
    private String targetType;

    private Long targetId;

    @Size(max = 180)
    private String targetTitle;

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public void setTargetTitle(String targetTitle) {
        this.targetTitle = targetTitle;
    }
}
