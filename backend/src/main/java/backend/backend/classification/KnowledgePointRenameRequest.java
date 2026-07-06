package backend.backend.classification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class KnowledgePointRenameRequest {

    @NotBlank(message = "原知识点名称不能为空")
    @Size(max = 80, message = "知识点名称不能超过80个字符")
    private String oldName;

    @NotBlank(message = "新知识点名称不能为空")
    @Size(max = 80, message = "知识点名称不能超过80个字符")
    private String newName;

    @Size(max = 80, message = "科目名称不能超过80个字符")
    private String subject;

    public String getOldName() {
        return oldName;
    }

    public void setOldName(String oldName) {
        this.oldName = oldName;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
