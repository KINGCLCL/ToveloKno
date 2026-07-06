package backend.backend.classification;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CategorySaveRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 80, message = "分类名称不能超过80个字符")
    private String name;

    @Size(max = 255, message = "分类描述不能超过255个字符")
    private String description;

    private Long parentId;

    @NotNull(message = "分类状态不能为空")
    private Boolean active = true;

    @NotNull(message = "排序值不能为空")
    @Min(value = 0, message = "排序值不能小于0")
    @Max(value = 9999, message = "排序值不能大于9999")
    private Integer sortOrder = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
