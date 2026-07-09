package backend.backend.learningresource;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.common.BusinessException;
import backend.backend.question.QuestionResponse;
import backend.backend.question.QuestionSaveRequest;
import backend.backend.question.QuestionService;
import backend.backend.question.QuestionStatus;
import backend.backend.question.QuestionType;
import backend.backend.service.OperationLogService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static backend.backend.learningresource.LearningResourceDtos.ExtractedQuestionPreview;
import static backend.backend.learningresource.LearningResourceDtos.QuestionExtractionResponse;

@Service
public class QuestionExtractionService {

    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 80;
    private static final int MAX_TEXT_CHARS = 240_000;
    private static final int MIN_CONTENT_LENGTH = 6;
    private static final String PLACEHOLDER_ANSWER = "待补充";
    private static final Pattern QUESTION_START = Pattern.compile(
            "^(?:第\\s*)?(\\d{1,3})(?:\\s*[、.．)）:]|\\s*题[、.．:]?)\\s*(.*)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern QUESTION_START_Q = Pattern.compile(
            "^[Qq](\\d{1,3})\\s*[:：.．)）]\\s*(.*)$");
    private static final Pattern OPTION_LINE = Pattern.compile(
            "^([A-Ha-h])\\s*[.．、)）:]\\s*(.+)$");
    private static final Pattern INLINE_OPTION = Pattern.compile(
            "(?<![A-Za-z0-9])([A-Ha-h])\\s*[.．、)）:]\\s*([^A-Ha-h\\n\\r]{1,500}?)(?=\\s+[A-Ha-h]\\s*[.．、)）:]|$)");
    private static final Pattern ANSWER_LINE = Pattern.compile(
            "^(?:参考答案|正确答案|答案|Answer|答案解析)\\s*[:：]\\s*(.*)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ANALYSIS_LINE = Pattern.compile(
            "^(?:解析|分析|解题思路|Explanation)\\s*[:：]\\s*(.*)$",
            Pattern.CASE_INSENSITIVE);

    private final LearningResourceRepository learningResourceRepository;
    private final QuestionService questionService;
    private final OperationLogService operationLogService;
    private final Path uploadRoot;

    public QuestionExtractionService(
            LearningResourceRepository learningResourceRepository,
            QuestionService questionService,
            OperationLogService operationLogService,
            @Value("${app.upload.resource-dir:uploads/resources}") String uploadDir) {
        this.learningResourceRepository = learningResourceRepository;
        this.questionService = questionService;
        this.operationLogService = operationLogService;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional
    public QuestionExtractionResponse extractAndCreate(
            Long resourceId,
            AuthenticatedUser currentUser,
            Integer requestedLimit,
            QuestionStatus requestedStatus) {
        LearningResource resource = learningResourceRepository.findByIdAndUserId(resourceId, currentUser.getId())
                .orElseThrow(() -> new BusinessException("学习资料不存在或无权访问"));
        int limit = normalizeLimit(requestedLimit);
        List<String> warnings = new ArrayList<>();
        String text = extractText(resource, warnings);
        if (text.isBlank()) {
            throw new BusinessException("未能从该资料中读取到文字内容，请确认 PDF/Word 不是纯图片扫描件");
        }

        List<QuestionCandidate> candidates = parseCandidates(text, resource, limit, warnings);
        List<QuestionResponse> created = new ArrayList<>();
        List<ExtractedQuestionPreview> previews = new ArrayList<>();
        QuestionStatus status = requestedStatus == null ? QuestionStatus.DRAFT : requestedStatus;
        Set<String> seen = new LinkedHashSet<>();
        for (QuestionCandidate candidate : candidates) {
            if (created.size() >= limit) {
                break;
            }
            String fingerprint = fingerprint(candidate.content());
            if (!seen.add(fingerprint)) {
                continue;
            }
            QuestionSaveRequest request = toQuestionRequest(candidate, resource, status);
            QuestionResponse response = questionService.createQuestion(request, currentUser);
            created.add(response);
            previews.add(toPreview(candidate, response.getId()));
        }

        if (created.isEmpty()) {
            warnings.add("没有找到可入库的题目。建议使用“1. 题干 / A. 选项 / 答案：A / 解析：...”这类格式。");
        }
        operationLogService.record(
                currentUser.getId(),
                "RESOURCE_AI_EXTRACT_QUESTIONS",
                "从学习资料自动识别题目：" + resource.getName() + "，入库 " + created.size() + " 题");
        return new QuestionExtractionResponse(
                resource.getId(),
                resource.getName(),
                created.size(),
                candidates.size(),
                warnings,
                previews,
                created
        );
    }

    public ExtractedResourceText extractResourceText(Long resourceId, AuthenticatedUser currentUser) {
        LearningResource resource = learningResourceRepository.findByIdAndUserId(resourceId, currentUser.getId())
                .orElseThrow(() -> new BusinessException("学习资料不存在或无权访问"));
        List<String> warnings = new ArrayList<>();
        String text = extractText(resource, warnings);
        return new ExtractedResourceText(resource.getId(), resource.getName(), text, warnings);
    }

    private String extractText(LearningResource resource, List<String> warnings) {
        Path path = uploadRoot.resolve(resource.getStoredFilename()).normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new BusinessException("资料文件路径不合法");
        }
        if (!Files.exists(path)) {
            throw new BusinessException("资料文件不存在，请重新上传");
        }
        String extension = extensionFrom(resource.getOriginalFilename());
        try {
            String text = switch (extension) {
                case ".pdf" -> extractPdf(path, warnings);
                case ".docx" -> extractDocx(path);
                case ".doc" -> extractDoc(path);
                case ".txt", ".md" -> Files.readString(path, StandardCharsets.UTF_8);
                default -> throw new BusinessException("当前仅支持从 PDF、Word、TXT、Markdown 资料中识别题目");
            };
            return normalizeDocumentText(text);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException("资料文字读取失败，请确认文件未损坏");
        } catch (RuntimeException exception) {
            throw new BusinessException("资料解析失败：" + exception.getMessage());
        }
    }

    private String extractPdf(Path path, List<String> warnings) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            if (document.isEncrypted()) {
                throw new BusinessException("暂不支持加密 PDF，请先解除密码保护");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text.isBlank()) {
                warnings.add("PDF 可能是扫描图片，当前只能识别可复制文本。");
            }
            return text;
        }
    }

    private String extractDocx(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                appendLine(text, paragraph.getText());
            }
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        appendLine(text, cell.getText());
                    }
                }
            }
            return text.toString();
        }
    }

    private String extractDoc(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private List<QuestionCandidate> parseCandidates(
            String text,
            LearningResource resource,
            int limit,
            List<String> warnings) {
        List<String> lines = text.lines()
                .map(this::normalizeLine)
                .filter(line -> !line.isBlank())
                .toList();
        List<QuestionBlock> blocks = splitQuestionBlocks(lines);
        if (blocks.isEmpty()) {
            warnings.add("未检测到明显题号，已尝试按空行和问号做保守拆分。");
            blocks = fallbackBlocks(text);
        }

        List<QuestionCandidate> candidates = new ArrayList<>();
        for (QuestionBlock block : blocks) {
            Optional<QuestionCandidate> candidate = parseBlock(block, resource);
            candidate.ifPresent(candidates::add);
            if (candidates.size() >= limit) {
                break;
            }
        }
        return candidates;
    }

    private List<QuestionBlock> splitQuestionBlocks(List<String> lines) {
        List<QuestionBlock> blocks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        Integer currentNumber = null;
        for (String line : lines) {
            QuestionHeader header = questionHeader(line);
            if (header != null) {
                if (!current.isEmpty()) {
                    blocks.add(new QuestionBlock(currentNumber, current));
                }
                current = new ArrayList<>();
                currentNumber = header.number();
                if (!header.remainder().isBlank()) {
                    current.add(header.remainder());
                }
            } else if (!current.isEmpty()) {
                current.add(line);
            }
        }
        if (!current.isEmpty()) {
            blocks.add(new QuestionBlock(currentNumber, current));
        }
        return blocks;
    }

    private List<QuestionBlock> fallbackBlocks(String text) {
        List<QuestionBlock> blocks = new ArrayList<>();
        String normalized = text.replaceAll("\\r\\n?", "\n");
        String[] chunks = normalized.split("\\n\\s*\\n+");
        int index = 1;
        for (String chunk : chunks) {
            String compact = chunk.replaceAll("\\s+", " ").trim();
            if (compact.length() >= MIN_CONTENT_LENGTH && looksLikeQuestion(compact)) {
                blocks.add(new QuestionBlock(index++, List.of(compact)));
            }
            if (blocks.size() >= DEFAULT_LIMIT) {
                break;
            }
        }
        return blocks;
    }

    private Optional<QuestionCandidate> parseBlock(QuestionBlock block, LearningResource resource) {
        List<String> contentLines = new ArrayList<>();
        List<String> options = new ArrayList<>();
        String answer = null;
        StringBuilder analysis = new StringBuilder();
        boolean inAnalysis = false;

        for (String rawLine : block.lines()) {
            String line = normalizeLine(rawLine);
            Matcher answerMatcher = ANSWER_LINE.matcher(line);
            Matcher analysisMatcher = ANALYSIS_LINE.matcher(line);
            Matcher optionMatcher = OPTION_LINE.matcher(line);
            if (answerMatcher.matches()) {
                String extracted = answerMatcher.group(1).trim();
                if (line.startsWith("答案解析") || line.toLowerCase(Locale.ROOT).startsWith("answer explanation")) {
                    appendLine(analysis, extracted);
                    inAnalysis = true;
                } else {
                    answer = extracted.isBlank() ? answer : normalizeAnswer(extracted);
                    inAnalysis = false;
                }
            } else if (analysisMatcher.matches()) {
                appendLine(analysis, analysisMatcher.group(1));
                inAnalysis = true;
            } else if (optionMatcher.matches()) {
                options.add(formatOption(optionMatcher.group(1), optionMatcher.group(2)));
                inAnalysis = false;
            } else if (inAnalysis) {
                appendLine(analysis, line);
            } else {
                List<String> inlineOptions = extractInlineOptions(line);
                if (inlineOptions.size() >= 2) {
                    String questionPart = stripInlineOptions(line).trim();
                    if (!questionPart.isBlank()) {
                        contentLines.add(questionPart);
                    }
                    options.addAll(inlineOptions);
                } else {
                    contentLines.add(line);
                }
            }
        }

        String content = normalizeContent(String.join("\n", contentLines));
        if (content.length() < MIN_CONTENT_LENGTH) {
            return Optional.empty();
        }

        List<String> normalizedOptions = normalizeOptions(options);
        QuestionType questionType = inferType(content, normalizedOptions, answer);
        if ((questionType == QuestionType.SINGLE_CHOICE || questionType == QuestionType.MULTIPLE_CHOICE)
                && normalizedOptions.size() < 2) {
            questionType = QuestionType.SHORT_ANSWER;
            normalizedOptions = List.of();
        }
        String correctAnswer = answer == null || answer.isBlank() ? PLACEHOLDER_ANSWER : answer;
        String sourceExcerpt = truncate(String.join("\n", block.lines()), 4000);
        return Optional.of(new QuestionCandidate(
                content,
                questionType,
                normalizedOptions,
                correctAnswer,
                normalizeContent(analysis.toString()),
                3,
                subjectFrom(resource),
                knowledgePointFrom(resource),
                block.number(),
                sourceExcerpt
        ));
    }

    private QuestionSaveRequest toQuestionRequest(
            QuestionCandidate candidate,
            LearningResource resource,
            QuestionStatus status) {
        QuestionSaveRequest request = new QuestionSaveRequest();
        request.setContent(truncate(candidate.content(), 10000));
        request.setQuestionType(candidate.questionType());
        request.setOptions(candidate.options());
        request.setCorrectAnswer(truncate(candidate.correctAnswer(), 1000));
        request.setAnalysis(truncate(blankToNull(candidate.analysis()), 10000));
        request.setDifficulty(candidate.difficulty());
        request.setSubject(truncate(candidate.subject(), 80));
        request.setKnowledgePoint(truncate(candidate.knowledgePoint(), 80));
        request.setStatus(status);
        request.setSourceType("RESOURCE_AI_EXTRACT");
        request.setSourceResourceId(resource.getId());
        request.setSourceResourceName(resource.getName());
        request.setSourcePage(null);
        request.setSourceExcerpt(candidate.sourceExcerpt());
        return request;
    }

    private ExtractedQuestionPreview toPreview(QuestionCandidate candidate, Long questionId) {
        return new ExtractedQuestionPreview(
                questionId,
                candidate.content(),
                candidate.questionType(),
                candidate.options(),
                candidate.correctAnswer(),
                blankToNull(candidate.analysis()),
                candidate.difficulty(),
                candidate.subject(),
                candidate.knowledgePoint(),
                candidate.sourcePage(),
                candidate.sourceExcerpt()
        );
    }

    private QuestionHeader questionHeader(String line) {
        Matcher matcher = QUESTION_START.matcher(line);
        if (matcher.matches()) {
            return new QuestionHeader(Integer.parseInt(matcher.group(1)), matcher.group(2).trim());
        }
        Matcher qMatcher = QUESTION_START_Q.matcher(line);
        if (qMatcher.matches()) {
            return new QuestionHeader(Integer.parseInt(qMatcher.group(1)), qMatcher.group(2).trim());
        }
        return null;
    }

    private QuestionType inferType(String content, List<String> options, String answer) {
        String compact = content.replaceAll("\\s+", "");
        if (options.size() >= 2) {
            String normalizedAnswer = answer == null ? "" : answer.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
            if (normalizedAnswer.matches("[A-H]{2,}|[A-H](,[A-H])+")) {
                return QuestionType.MULTIPLE_CHOICE;
            }
            return QuestionType.SINGLE_CHOICE;
        }
        if (compact.contains("判断") || matchesAny(answer, "正确", "错误", "对", "错", "true", "false")) {
            return QuestionType.TRUE_FALSE;
        }
        if (compact.contains("____") || compact.contains("（）") || compact.contains("( )") || compact.contains("填空")) {
            return QuestionType.FILL_BLANK;
        }
        return QuestionType.SHORT_ANSWER;
    }

    private List<String> normalizeOptions(List<String> options) {
        List<String> normalized = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String option : options) {
            String clean = truncate(normalizeContent(option), 500);
            if (!clean.isBlank() && seen.add(clean.toLowerCase(Locale.ROOT))) {
                normalized.add(clean);
            }
            if (normalized.size() >= 8) {
                break;
            }
        }
        return normalized;
    }

    private List<String> extractInlineOptions(String line) {
        List<String> options = new ArrayList<>();
        Matcher matcher = INLINE_OPTION.matcher(line);
        while (matcher.find()) {
            String value = matcher.group(2).trim();
            if (!value.isBlank()) {
                options.add(formatOption(matcher.group(1), value));
            }
        }
        return options;
    }

    private String stripInlineOptions(String line) {
        Matcher matcher = INLINE_OPTION.matcher(line);
        int start = line.length();
        if (matcher.find()) {
            start = matcher.start();
        }
        return line.substring(0, start);
    }

    private String normalizeAnswer(String value) {
        String answer = value.replaceAll("^[（(]?\\s*", "").replaceAll("\\s*[)）]?$", "").trim();
        if (answer.matches("(?i)[A-H](\\s*[,，、]\\s*[A-H])+")) {
            return answer.toUpperCase(Locale.ROOT).replaceAll("\\s*[，、]\\s*", ",").replaceAll("\\s+", "");
        }
        if (answer.matches("(?i)[A-H]+")) {
            return answer.toUpperCase(Locale.ROOT);
        }
        return answer;
    }

    private String normalizeDocumentText(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC);
        normalized = normalized.replace('\u00A0', ' ');
        normalized = normalized.replace("\uFEFF", "");
        if (normalized.length() > MAX_TEXT_CHARS) {
            normalized = normalized.substring(0, MAX_TEXT_CHARS);
        }
        return normalized;
    }

    private String normalizeLine(String line) {
        return line == null ? "" : line.replace("\uFEFF", "").replaceAll("\\s+", " ").trim();
    }

    private String normalizeContent(String value) {
        return value == null ? "" : value.replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private String formatOption(String label, String value) {
        return label.toUpperCase(Locale.ROOT) + ". " + value.trim();
    }

    private boolean looksLikeQuestion(String value) {
        return value.contains("?")
                || value.contains("？")
                || value.contains("答案")
                || value.contains("解析")
                || value.matches(".*[A-Ha-h]\\s*[.．、)）:]\\s*.+");
    }

    private boolean matchesAny(String value, String... targets) {
        if (value == null) {
            return false;
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        for (String target : targets) {
            if (lower.equals(target.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String subjectFrom(LearningResource resource) {
        return "学习资料";
    }

    private String knowledgePointFrom(LearningResource resource) {
        String name = resource.getName() == null ? "" : resource.getName();
        int dotIndex = name.lastIndexOf('.');
        String clean = dotIndex > 0 ? name.substring(0, dotIndex) : name;
        return truncate(clean.trim(), 80);
    }

    private String extensionFrom(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex < 0 ? "" : filename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(MAX_LIMIT, requestedLimit));
    }

    private String fingerprint(String value) {
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void appendLine(StringBuilder builder, String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(line.trim());
    }

    private record QuestionHeader(Integer number, String remainder) {
    }

    private record QuestionBlock(Integer number, List<String> lines) {
    }

    private record QuestionCandidate(
            String content,
            QuestionType questionType,
            List<String> options,
            String correctAnswer,
            String analysis,
            Integer difficulty,
            String subject,
            String knowledgePoint,
            Integer sourcePage,
            String sourceExcerpt) {
    }

    public record ExtractedResourceText(
            Long resourceId,
            String resourceName,
            String text,
            List<String> warnings) {
    }
}
