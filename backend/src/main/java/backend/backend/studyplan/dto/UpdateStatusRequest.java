package backend.backend.studyplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 修改学习计划状态的请求体。
 */
public class UpdateStatusRequest {

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "pending|completed|cancelled", message = "状态值必须为 pending、completed 或 cancelled")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
