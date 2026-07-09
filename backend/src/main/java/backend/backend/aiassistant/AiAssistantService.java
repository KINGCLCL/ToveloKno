package backend.backend.aiassistant;

import backend.backend.aiassistant.AiAssistantDtos.AiAnalysisRequest;
import backend.backend.aiassistant.AiAssistantDtos.AiAnalysisResponse;
import backend.backend.aiassistant.AiAssistantDtos.AiAssistantStatusResponse;
import backend.backend.aiassistant.AiAssistantDtos.AiKnowledgePoint;
import backend.backend.aiassistant.AiAssistantDtos.AiPlanDraft;
import backend.backend.aiassistant.AiAssistantDtos.AiQuestionDraft;
import backend.backend.auth.AuthenticatedUser;
import backend.backend.common.BusinessException;
import backend.backend.learningresource.QuestionExtractionService;
import backend.backend.learningresource.QuestionExtractionService.ExtractedResourceText;
import backend.backend.question.QuestionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiAssistantService {

    private static final int MAX_CONTEXT_CHARS = 18000;
    private static final int MAX_SOURCE_EXCERPT = 1200;

    private final QuestionExtractionService questionExtractionService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String provider;
    private final String model;

    public AiAssistantService(
            QuestionExtractionService questionExtractionService,
            ObjectMapper objectMapper,
            @Value("${app.ai.deepseek.api-key:}") String apiKey,
            @Value("${app.ai.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${app.ai.deepseek.model:deepseek-v4-flash}") String model) {
        this.questionExtractionService = questionExtractionService;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.provider = "DeepSeek";
        this.model = model == null || model.isBlank() ? "deepseek-v4-flash" : model.trim();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl == null || baseUrl.isBlank() ? "https://api.deepseek.com" : baseUrl.trim())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public AiAssistantStatusResponse status() {
        return new AiAssistantStatusResponse(configured(), provider, model);
    }

    public AiAnalysisResponse analyze(AiAnalysisRequest request, AuthenticatedUser currentUser) {
        if (!configured()) {
            throw new BusinessException("DeepSeek API Key 未配置，请设置环境变量 DEEPSEEK_API_KEY 后重启后端");
        }
        SourceContext context = sourceContext(request, currentUser);
        if (context.text().isBlank()) {
            throw new BusinessException("没有可分析的文字内容，请选择可复制文字的资料或粘贴文本");
        }
        String prompt = buildPrompt(request, context);
        DeepSeekResponse response = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(new DeepSeekRequest(
                        model,
                        List.of(
                                new ChatMessage("system", systemPrompt()),
                                new ChatMessage("user", prompt)
                        ),
                        0.2,
                        0.95,
                        4096,
                        new ResponseFormat("json_object"),
                        new Thinking("disabled")))
                .retrieve()
                .body(DeepSeekResponse.class);
        String content = response == null || response.choices() == null || response.choices().isEmpty()
                ? ""
                : response.choices().getFirst().message().content();
        if (content == null || content.isBlank()) {
            throw new BusinessException("DeepSeek 未返回有效内容，请稍后重试");
        }
        AiModelPayload payload = parsePayload(content);
        List<String> warnings = new ArrayList<>(context.warnings());
        if (payload.warnings() != null) {
            warnings.addAll(payload.warnings());
        }
        return new AiAnalysisResponse(
                configured(),
                provider,
                model,
                context.title(),
                truncate(context.text(), MAX_SOURCE_EXCERPT),
                normalizeQuestions(payload.questions(), request, context),
                normalizeKnowledge(payload.knowledgePoints()),
                normalizePlans(payload.plans(), request),
                warnings
        );
    }

    private boolean configured() {
        return !apiKey.isBlank();
    }

    private SourceContext sourceContext(AiAnalysisRequest request, AuthenticatedUser currentUser) {
        List<String> warnings = new ArrayList<>();
        String pasted = request.text() == null ? "" : request.text().trim();
        if (!pasted.isBlank()) {
            return new SourceContext("粘贴文本", truncate(pasted, MAX_CONTEXT_CHARS), warnings, null);
        }
        if (request.resourceId() == null) {
            throw new BusinessException("请选择学习资料或粘贴需要 AI 处理的文字");
        }
        ExtractedResourceText extracted = questionExtractionService.extractResourceText(request.resourceId(), currentUser);
        warnings.addAll(extracted.warnings());
        if (extracted.text() != null && extracted.text().length() > MAX_CONTEXT_CHARS) {
            warnings.add("资料文字较长，已截取前 " + MAX_CONTEXT_CHARS + " 字用于 AI 分析。");
        }
        return new SourceContext(
                extracted.resourceName(),
                truncate(extracted.text(), MAX_CONTEXT_CHARS),
                warnings,
                extracted.resourceId());
    }

    private String systemPrompt() {
        return """
                你是一个学习资料处理助手。只返回严格 JSON，不要 Markdown，不要解释。
                你需要从资料中生成可入库题目、知识要点和学习计划建议。
                题目不要编造资料中没有依据的内容；不确定答案时 correctAnswer 使用“待补充”。
                """;
    }

    private String buildPrompt(AiAnalysisRequest request, SourceContext context) {
        int questionLimit = request.questionLimit() == null ? 6 : Math.max(1, Math.min(20, request.questionLimit()));
        int planDays = request.planDays() == null ? 3 : Math.max(1, Math.min(14, request.planDays()));
        LocalDate startDate = request.planStartDate() == null ? LocalDate.now() : request.planStartDate();
        return """
                请基于下面资料生成学习辅助结果。
                输出 JSON schema:
                {
                  "questions": [
                    {
                      "content": "题干",
                      "questionType": "SINGLE_CHOICE|MULTIPLE_CHOICE|TRUE_FALSE|FILL_BLANK|SHORT_ANSWER",
                      "options": ["A. ..."],
                      "correctAnswer": "答案",
                      "analysis": "解析",
                      "difficulty": 1-5,
                      "subject": "科目",
                      "knowledgePoint": "知识点",
                      "sourceExcerpt": "资料依据摘录"
                    }
                  ],
                  "knowledgePoints": [
                    {
                      "title": "知识点名称",
                      "summary": "一句话总结",
                      "keyItems": ["要点"],
                      "pitfalls": ["易错点"],
                      "priority": 1-5
                    }
                  ],
                  "plans": [
                    {
                      "title": "计划标题",
                      "content": "具体任务",
                      "planDate": "YYYY-MM-DD",
                      "targetType": "RESOURCE|QUESTION|REVIEW",
                      "targetTitle": "关联对象"
                    }
                  ],
                  "warnings": ["注意事项"]
                }
                                
                要求:
                - questions 最多 %d 题。
                - plans 覆盖从 %s 开始的 %d 天。
                - subject 优先使用：%s。
                - knowledgePoint 优先使用：%s。
                - 单选/多选题 options 至少 2 个；非选择题 options 为空数组。
                                
                资料标题：%s
                资料内容：
                %s
                """.formatted(
                questionLimit,
                startDate,
                planDays,
                blankToDefault(request.subject(), "学习资料"),
                blankToDefault(request.knowledgePoint(), "由资料判断"),
                context.title(),
                context.text());
    }

    private AiModelPayload parsePayload(String content) {
        String json = extractJson(content);
        try {
            return objectMapper.readValue(json, AiModelPayload.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("AI 返回内容格式不正确，请重试或减少输入文本长度");
        }
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        int first = trimmed.indexOf('{');
        int last = trimmed.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return trimmed.substring(first, last + 1);
        }
        return trimmed;
    }

    private List<AiQuestionDraft> normalizeQuestions(
            List<AiQuestionDraft> questions,
            AiAnalysisRequest request,
            SourceContext context) {
        if (questions == null) {
            return List.of();
        }
        int limit = request.questionLimit() == null ? 6 : Math.max(1, Math.min(20, request.questionLimit()));
        return questions.stream()
                .filter(item -> item.content() != null && !item.content().isBlank())
                .limit(limit)
                .map(item -> new AiQuestionDraft(
                        truncate(item.content(), 10000),
                        item.questionType() == null ? QuestionType.SHORT_ANSWER : item.questionType(),
                        item.options() == null ? List.of() : item.options().stream().filter(option -> option != null && !option.isBlank()).limit(8).toList(),
                        blankToDefault(item.correctAnswer(), "待补充"),
                        truncate(item.analysis(), 10000),
                        item.difficulty() == null ? 3 : Math.max(1, Math.min(5, item.difficulty())),
                        blankToDefault(item.subject(), blankToDefault(request.subject(), "学习资料")),
                        truncate(blankToDefault(item.knowledgePoint(), request.knowledgePoint()), 80),
                        truncate(blankToDefault(item.sourceExcerpt(), context.text()), MAX_SOURCE_EXCERPT)))
                .toList();
    }

    private List<AiKnowledgePoint> normalizeKnowledge(List<AiKnowledgePoint> points) {
        if (points == null) {
            return List.of();
        }
        return points.stream()
                .filter(item -> item.title() != null && !item.title().isBlank())
                .limit(12)
                .map(item -> new AiKnowledgePoint(
                        truncate(item.title(), 80),
                        truncate(item.summary(), 600),
                        item.keyItems() == null ? List.of() : item.keyItems().stream().filter(value -> value != null && !value.isBlank()).limit(8).toList(),
                        item.pitfalls() == null ? List.of() : item.pitfalls().stream().filter(value -> value != null && !value.isBlank()).limit(8).toList(),
                        item.priority() == null ? 3 : Math.max(1, Math.min(5, item.priority()))))
                .toList();
    }

    private List<AiPlanDraft> normalizePlans(List<AiPlanDraft> plans, AiAnalysisRequest request) {
        if (plans == null) {
            return List.of();
        }
        LocalDate startDate = request.planStartDate() == null ? LocalDate.now() : request.planStartDate();
        return plans.stream()
                .filter(item -> item.title() != null && !item.title().isBlank())
                .limit(14)
                .map(item -> new AiPlanDraft(
                        truncate(item.title(), 150),
                        truncate(item.content(), 2000),
                        item.planDate() == null ? startDate : item.planDate(),
                        blankToDefault(item.targetType(), "RESOURCE"),
                        truncate(item.targetTitle(), 180)))
                .toList();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private record SourceContext(String title, String text, List<String> warnings, Long resourceId) {
    }

    private record DeepSeekRequest(
            String model,
            List<ChatMessage> messages,
            double temperature,
            double top_p,
            int max_tokens,
            ResponseFormat response_format,
            Thinking thinking) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ResponseFormat(String type) {
    }

    private record Thinking(String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DeepSeekResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(ChatMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiModelPayload(
            List<AiQuestionDraft> questions,
            List<AiKnowledgePoint> knowledgePoints,
            List<AiPlanDraft> plans,
            List<String> warnings) {
    }
}
