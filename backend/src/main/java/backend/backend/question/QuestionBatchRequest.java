package backend.backend.question;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量题目操作请求。
 */
public class QuestionBatchRequest {

    @NotEmpty(message = "请选择至少一道题目")
    @Size(max = 100, message = "单次最多操作100道题目")
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
