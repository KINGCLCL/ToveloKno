package backend.backend.question;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新增、修改题目共用的请求参数。
 */
public class QuestionSaveRequest {

    @NotBlank(message = "题目内容不能为空")
    @Size(max = 10000, message = "题目内容不能超过10000个字符")
    private String content;

    @NotNull(message = "题型不能为空")
    private QuestionType questionType;

    @Size(max = 8, message = "选项不能超过8个")
    private List<@NotBlank(message = "选项内容不能为空") @Size(max = 500, message = "单个选项不能超过500个字符") String> options;

    @NotBlank(message = "正确答案不能为空")
    @Size(max = 1000, message = "正确答案不能超过1000个字符")
    private String correctAnswer;

    @Size(max = 10000, message = "答案解析不能超过10000个字符")
    private String analysis;

    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度不能小于1")
    @Max(value = 5, message = "难度不能大于5")
    private Integer difficulty;

    @Size(max = 80, message = "科目名称不能超过80个字符")
    private String subject;

    @Size(max = 80, message = "知识点名称不能超过80个字符")
    private String knowledgePoint;

    @NotNull(message = "题目状态不能为空")
    private QuestionStatus status;

    private Long categoryId;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getKnowledgePoint() {
        return knowledgePoint;
    }

    public void setKnowledgePoint(String knowledgePoint) {
        this.knowledgePoint = knowledgePoint;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
