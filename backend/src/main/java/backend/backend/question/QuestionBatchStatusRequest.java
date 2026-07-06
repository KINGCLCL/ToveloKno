package backend.backend.question;
import jakarta.validation.constraints.NotNull;

/**
 * 批量修改发布状态请求。
 */
public class QuestionBatchStatusRequest extends QuestionBatchRequest {

    @NotNull(message = "题目状态不能为空")
    private QuestionStatus status;

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }
}
