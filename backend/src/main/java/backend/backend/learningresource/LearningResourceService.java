package backend.backend.learningresource;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.common.BusinessException;
import backend.backend.common.PageResponse;
import backend.backend.learningresource.LearningResourceDtos.AnnotationPayload;
import backend.backend.learningresource.LearningResourceDtos.ResourceImageQuestionRequest;
import backend.backend.learningresource.LearningResourceDtos.ResourceQuestionRequest;
import backend.backend.learningresource.LearningResourceDtos.ResourceResponse;
import backend.backend.question.QuestionResponse;
import backend.backend.question.QuestionSaveRequest;
import backend.backend.question.QuestionService;
import backend.backend.question.QuestionStatus;
import backend.backend.question.QuestionType;
import backend.backend.service.OperationLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LearningResourceService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx", ".txt", ".md",
            ".png", ".jpg", ".jpeg", ".webp", ".gif", ".mp4", ".mp3", ".wav");
    private static final TypeReference<List<AnnotationPayload>> ANNOTATION_LIST_TYPE = new TypeReference<>() {
    };

    private final LearningResourceRepository learningResourceRepository;
    private final QuestionService questionService;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;
    private final Path uploadRoot;

    public LearningResourceService(
            LearningResourceRepository learningResourceRepository,
            QuestionService questionService,
            OperationLogService operationLogService,
            ObjectMapper objectMapper,
            @Value("${app.upload.resource-dir:uploads/resources}") String uploadDir) {
        this.learningResourceRepository = learningResourceRepository;
        this.questionService = questionService;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> listResources(
            AuthenticatedUser currentUser,
            String keyword,
            String type,
            Boolean favorite,
            int page,
            int size) {
        Specification<LearningResource> specification = ownerIs(currentUser.getId())
                .and(keywordContains(keyword))
                .and(typeEquals(type))
                .and(favoriteEquals(favorite));
        Page<ResourceResponse> result = learningResourceRepository.findAll(
                specification,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        ).map(this::toResponse);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResource(Long resourceId, AuthenticatedUser currentUser) {
        return toResponse(findOwned(resourceId, currentUser.getId()));
    }

    @Transactional
    public ResourceResponse uploadResource(
            AuthenticatedUser currentUser,
            MultipartFile file,
            String name,
            String description) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的学习资料");
        }

        String originalFilename = normalizeFilename(file.getOriginalFilename());
        String extension = extensionFrom(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("暂不支持该文件类型");
        }

        String storedFilename = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(uploadRoot);
            file.transferTo(uploadRoot.resolve(storedFilename));
        } catch (IOException exception) {
            throw new IllegalStateException("学习资料保存失败");
        }

        LocalDateTime now = LocalDateTime.now();
        LearningResource resource = new LearningResource();
        resource.setUserId(currentUser.getId());
        resource.setName(normalizeTitle(name, originalFilename));
        resource.setType(typeFrom(extension));
        resource.setMimeType(file.getContentType());
        resource.setOriginalFilename(originalFilename);
        resource.setStoredFilename(storedFilename);
        resource.setFileUrl("/uploads/resources/" + storedFilename);
        resource.setFileSize(file.getSize());
        resource.setSource("本地上传");
        resource.setDescription(normalizeDescription(description));
        resource.setFavorite(false);
        resource.setProgressPercent(0);
        resource.setCurrentPage(1);
        resource.setTotalPages(0);
        resource.setLearnedMinutes(0);
        resource.setAnnotationCount(0);
        resource.setAnnotationsJson("[]");
        resource.setCreatedAt(now);
        resource.setUpdatedAt(now);

        LearningResource saved = learningResourceRepository.save(resource);
        operationLogService.record(currentUser.getId(), "RESOURCE_UPLOAD", "上传学习资料：" + saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public ResourceResponse updateProgress(
            Long resourceId,
            AuthenticatedUser currentUser,
            LearningResourceDtos.ProgressRequest request) {
        LearningResource resource = findOwned(resourceId, currentUser.getId());
        resource.setProgressPercent(request.progressPercent());
        resource.setCurrentPage(request.currentPage());
        resource.setTotalPages(request.totalPages());
        resource.setLearnedMinutes(request.learnedMinutes());
        resource.setLastStudiedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        return toResponse(learningResourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse updateAnnotations(
            Long resourceId,
            AuthenticatedUser currentUser,
            List<AnnotationPayload> annotations) {
        LearningResource resource = findOwned(resourceId, currentUser.getId());
        String json = writeAnnotations(annotations);
        resource.setAnnotationsJson(json);
        resource.setAnnotationCount(annotations == null ? 0 : annotations.size());
        resource.setLastStudiedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        return toResponse(learningResourceRepository.save(resource));
    }

    @Transactional
    public QuestionResponse createQuestionFromExcerpt(
            Long resourceId,
            AuthenticatedUser currentUser,
            ResourceQuestionRequest request) {
        LearningResource resource = findOwned(resourceId, currentUser.getId());

        QuestionSaveRequest questionRequest = new QuestionSaveRequest();
        questionRequest.setContent(request.content());
        questionRequest.setQuestionType(request.questionType());
        questionRequest.setOptions(request.options());
        questionRequest.setCorrectAnswer(request.correctAnswer());
        questionRequest.setAnalysis(request.analysis());
        questionRequest.setDifficulty(request.difficulty());
        questionRequest.setSubject(request.subject());
        questionRequest.setKnowledgePoint(request.knowledgePoint());
        questionRequest.setStatus(request.status() == null ? QuestionStatus.DRAFT : request.status());
        questionRequest.setCategoryId(request.categoryId());
        questionRequest.setSourceType("RESOURCE_EXCERPT");
        questionRequest.setSourceResourceId(resource.getId());
        questionRequest.setSourceResourceName(resource.getName());
        questionRequest.setSourcePage(request.sourcePage());
        questionRequest.setSourceExcerpt(request.sourceExcerpt());

        QuestionResponse created = questionService.createQuestion(questionRequest, currentUser);
        operationLogService.record(
                currentUser.getId(),
                "RESOURCE_EXCERPT_TO_QUESTION",
                "从学习资料生成题目：" + resource.getName());
        return created;
    }

    @Transactional
    public QuestionResponse createImageQuestion(
            Long resourceId,
            AuthenticatedUser currentUser,
            ResourceImageQuestionRequest request) {
        LearningResource resource = findOwned(resourceId, currentUser.getId());
        String imageSource = normalizeImageSource(request);
        String title = request.title() == null || request.title().isBlank()
                ? "根据图片资料作答"
                : request.title().trim();
        Integer sourcePage = request.sourcePage() == null ? null : Math.max(1, request.sourcePage());

        QuestionSaveRequest questionRequest = new QuestionSaveRequest();
        questionRequest.setContent(truncateText(title, 120) + "\n" + imageSource);
        questionRequest.setQuestionType(QuestionType.SHORT_ANSWER);
        questionRequest.setOptions(List.of());
        questionRequest.setCorrectAnswer("待补充");
        questionRequest.setAnalysis("由学习资料图片保存生成，请人工补充答案解析。");
        questionRequest.setDifficulty(3);
        questionRequest.setSubject("学习资料");
        questionRequest.setKnowledgePoint(truncateText(resource.getName(), 80));
        questionRequest.setStatus(request.status() == null ? QuestionStatus.DRAFT : request.status());
        questionRequest.setSourceType("RESOURCE_IMAGE_CAPTURE");
        questionRequest.setSourceResourceId(resource.getId());
        questionRequest.setSourceResourceName(resource.getName());
        questionRequest.setSourcePage(sourcePage);
        questionRequest.setSourceExcerpt(truncateText("图片题：" + resource.getName(), 4000));

        QuestionResponse created = questionService.createQuestion(questionRequest, currentUser);
        operationLogService.record(
                currentUser.getId(),
                "RESOURCE_IMAGE_TO_QUESTION",
                "从学习资料图片保存题目：" + resource.getName());
        return created;
    }

    @Transactional
    public ResourceResponse toggleFavorite(Long resourceId, AuthenticatedUser currentUser) {
        LearningResource resource = findOwned(resourceId, currentUser.getId());
        resource.setFavorite(!Boolean.TRUE.equals(resource.getFavorite()));
        resource.setUpdatedAt(LocalDateTime.now());
        return toResponse(learningResourceRepository.save(resource));
    }

    @Transactional
    public void deleteResource(Long resourceId, AuthenticatedUser currentUser) {
        LearningResource resource = findOwned(resourceId, currentUser.getId());
        learningResourceRepository.delete(resource);
        try {
            Files.deleteIfExists(uploadRoot.resolve(resource.getStoredFilename()));
        } catch (IOException ignored) {
            // Metadata deletion is the source of truth; stale files can be cleaned later.
        }
        operationLogService.record(currentUser.getId(), "RESOURCE_DELETE", "删除学习资料：" + resource.getName());
    }

    private LearningResource findOwned(Long resourceId, Long userId) {
        return learningResourceRepository.findByIdAndUserId(resourceId, userId)
                .orElseThrow(() -> new BusinessException("学习资料不存在或无权访问"));
    }

    private ResourceResponse toResponse(LearningResource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getMimeType(),
                resource.getOriginalFilename(),
                resource.getFileUrl(),
                resource.getFileSize(),
                formatSize(resource.getFileSize()),
                resource.getSource(),
                resource.getDescription(),
                Boolean.TRUE.equals(resource.getFavorite()),
                valueOrZero(resource.getProgressPercent()),
                Math.max(1, valueOrZero(resource.getCurrentPage())),
                valueOrZero(resource.getTotalPages()),
                valueOrZero(resource.getLearnedMinutes()),
                valueOrZero(resource.getAnnotationCount()),
                readAnnotations(resource.getAnnotationsJson()),
                resource.getLastStudiedAt(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }

    private Specification<LearningResource> ownerIs(Long userId) {
        return (root, query, builder) -> builder.equal(root.get("userId"), userId);
    }

    private Specification<LearningResource> keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("name")), pattern),
                builder.like(builder.lower(root.get("originalFilename")), pattern),
                builder.like(builder.lower(root.get("description")), pattern)
        );
    }

    private Specification<LearningResource> typeEquals(String type) {
        if (type == null || type.isBlank() || "全部".equals(type)) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("type"), type);
    }

    private Specification<LearningResource> favoriteEquals(Boolean favorite) {
        if (favorite == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("favorite"), favorite);
    }

    private String writeAnnotations(List<AnnotationPayload> annotations) {
        try {
            return objectMapper.writeValueAsString(annotations == null ? List.of() : annotations);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("标注数据格式不正确");
        }
    }

    private List<AnnotationPayload> readAnnotations(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, ANNOTATION_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private String normalizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "resource";
        }
        return Path.of(originalFilename).getFileName().toString();
    }

    private String extensionFrom(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String normalizeTitle(String name, String originalFilename) {
        String title = name == null || name.isBlank() ? originalFilename : name.trim();
        int maxLength = Math.min(title.length(), 180);
        return title.substring(0, maxLength);
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return "上传的学习资料，可记录阅读进度和标注。";
        }
        return description.trim().substring(0, Math.min(description.trim().length(), 800));
    }

    private String normalizeImageSource(ResourceImageQuestionRequest request) {
        String imageDataUrl = request.imageDataUrl() == null ? "" : request.imageDataUrl().trim();
        String imageUrl = request.imageUrl() == null ? "" : request.imageUrl().trim();
        if (!imageDataUrl.isBlank()) {
            if (!imageDataUrl.startsWith("data:image/")) {
                throw new BusinessException("图片数据格式不正确");
            }
            return "![资料图片](" + saveDataUrlImage(imageDataUrl) + ")";
        }
        if (!imageUrl.isBlank()) {
            return "![资料图片](" + imageUrl + ")";
        }
        throw new BusinessException("请先选择可保存为图片的资料页面");
    }

    private String saveDataUrlImage(String imageDataUrl) {
        int commaIndex = imageDataUrl.indexOf(',');
        if (commaIndex < 0) {
            throw new BusinessException("图片数据格式不正确");
        }
        String meta = imageDataUrl.substring(0, commaIndex).toLowerCase(Locale.ROOT);
        String extension = meta.contains("image/jpeg") || meta.contains("image/jpg") ? ".jpg" : ".png";
        String base64 = imageDataUrl.substring(commaIndex + 1);
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("图片数据解码失败");
        }
        if (bytes.length == 0) {
            throw new BusinessException("图片数据为空");
        }
        if (bytes.length > 2_000_000) {
            throw new BusinessException("图片过大，请放大到需要的区域后重新保存");
        }
        String storedFilename = "question-capture-" + UUID.randomUUID() + extension;
        try {
            Files.createDirectories(uploadRoot);
            Files.write(uploadRoot.resolve(storedFilename), bytes);
        } catch (IOException exception) {
            throw new IllegalStateException("图片题保存失败");
        }
        return "/uploads/resources/" + storedFilename;
    }

    private String truncateText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String typeFrom(String extension) {
        return switch (extension) {
            case ".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx", ".txt", ".md" -> "文档";
            case ".png", ".jpg", ".jpeg", ".webp", ".gif" -> "图片";
            case ".mp4" -> "视频";
            case ".mp3", ".wav" -> "音频";
            default -> "其他";
        };
    }

    private String formatSize(Long size) {
        if (size == null) {
            return "0 B";
        }
        double value = size;
        String[] units = {"B", "KB", "MB", "GB"};
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format(Locale.ROOT, value >= 10 ? "%.0f %s" : "%.1f %s", value, units[unit]);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
