package backend.backend.service;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.dto.TagRequest;
import backend.backend.dto.TagResponse;
import backend.backend.entity.Tag;
import backend.backend.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 标签模块业务层。
 *
 * 标签是全局唯一的轻量标记，后续资源和知识卡片模块可以复用。
 */
@Service
public class TagService {

    private final TagRepository tagRepository;
    private final OperationLogService operationLogService;

    public TagService(TagRepository tagRepository, OperationLogService operationLogService) {
        this.tagRepository = tagRepository;
        this.operationLogService = operationLogService;
    }

    /**
     * 查询标签列表。
     *
     * keyword 为空时返回全部标签；不为空时按标签名模糊查询。
     */
    public List<TagResponse> listTags(String keyword) {
        List<Tag> tags = isBlank(keyword)
                ? tagRepository.findAllByOrderByNameAsc()
                : tagRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword.trim());
        return tags.stream().map(this::toResponse).toList();
    }

    /**
     * 查询标签详情。
     */
    public TagResponse getTag(Long tagId) {
        return toResponse(findTagById(tagId));
    }

    /**
     * 新增标签。
     */
    @Transactional
    public TagResponse createTag(TagRequest request, AuthenticatedUser currentUser) {
        String name = normalizeRequiredText(request.getName(), "标签名称不能为空");
        String description = normalizeOptionalText(request.getDescription());

        if (tagRepository.existsByName(name)) {
            throw new IllegalArgumentException("标签名称已存在");
        }

        Tag tag = new Tag();
        tag.setName(name);
        tag.setDescription(description);
        tag.setCreatedBy(currentUser.getId());

        Tag savedTag = tagRepository.save(tag);
        operationLogService.record("TAG_CREATE", "新增标签：" + savedTag.getName());
        return toResponse(savedTag);
    }

    /**
     * 修改标签。
     */
    @Transactional
    public TagResponse updateTag(Long tagId, TagRequest request) {
        Tag tag = findTagById(tagId);
        String name = normalizeRequiredText(request.getName(), "标签名称不能为空");
        String description = normalizeOptionalText(request.getDescription());

        if (tagRepository.existsByNameAndIdNot(name, tagId)) {
            throw new IllegalArgumentException("标签名称已存在");
        }

        tag.setName(name);
        tag.setDescription(description);

        Tag savedTag = tagRepository.save(tag);
        operationLogService.record("TAG_UPDATE", "修改标签：" + savedTag.getName());
        return toResponse(savedTag);
    }

    /**
     * 删除标签。
     *
     * 标签被资源引用时，resource_tag 表会通过外键级联删除关联关系。
     */
    @Transactional
    public void deleteTag(Long tagId) {
        Tag tag = findTagById(tagId);
        tagRepository.delete(tag);
        operationLogService.record("TAG_DELETE", "删除标签：" + tag.getName());
    }

    private Tag findTagById(Long tagId) {
        return tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在"));
    }

    private TagResponse toResponse(Tag tag) {
        return new TagResponse(
                tag.getId(),
                tag.getName(),
                tag.getDescription(),
                tag.getCreatedBy(),
                tag.getCreatedAt()
        );
    }

    private String normalizeRequiredText(String text, String message) {
        if (isBlank(text)) {
            throw new IllegalArgumentException(message);
        }
        return text.trim();
    }

    private String normalizeOptionalText(String text) {
        if (isBlank(text)) {
            return null;
        }
        return text.trim();
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
