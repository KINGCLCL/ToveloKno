package backend.backend.controller;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import backend.backend.dto.TagRequest;
import backend.backend.dto.TagResponse;
import backend.backend.service.TagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签管理接口。
 *
 * 标签用于给资源和知识卡片添加检索标记。
 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    /**
     * 查询标签列表，支持按名称关键词搜索。
     */
    @GetMapping
    public ApiResponse<List<TagResponse>> listTags(@RequestParam(required = false) String keyword) {
        return ApiResponse.success("查询成功", tagService.listTags(keyword));
    }

    /**
     * 查询标签详情。
     */
    @GetMapping("/{tagId}")
    public ApiResponse<TagResponse> getTag(@PathVariable Long tagId) {
        return ApiResponse.success("查询成功", tagService.getTag(tagId));
    }

    /**
     * 新增标签。
     */
    @PostMapping
    public ApiResponse<TagResponse> createTag(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody TagRequest request) {
        return ApiResponse.success("新增成功", tagService.createTag(request, currentUser));
    }

    /**
     * 修改标签。
     */
    @PutMapping("/{tagId}")
    public ApiResponse<TagResponse> updateTag(
            @PathVariable Long tagId,
            @Valid @RequestBody TagRequest request) {
        return ApiResponse.success("修改成功", tagService.updateTag(tagId, request));
    }

    /**
     * 删除标签。
     */
    @DeleteMapping("/{tagId}")
    public ApiResponse<Void> deleteTag(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
        return ApiResponse.success("删除成功", null);
    }
}
