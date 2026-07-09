package backend.backend.practice;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.classification.ClassificationService;
import backend.backend.common.BusinessException;
import backend.backend.question.Question;
import backend.backend.question.QuestionRepository;
import backend.backend.question.QuestionStatus;
import backend.backend.question.QuestionType;
import backend.backend.service.OperationLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class QuestionBankInteractionService {

    private static final Set<String> PRACTICE_MODES =
            Set.of("free", "chapter", "random", "wrong", "weak", "challenge");

    private final AnswerRecordRepository answerRecordRepository;
    private final WrongQuestionRepository wrongQuestionRepository;
    private final QuestionBankSettingRepository settingRepository;
    private final QuestionRepository questionRepository;
    private final ClassificationService classificationService;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public QuestionBankInteractionService(
            AnswerRecordRepository answerRecordRepository,
            WrongQuestionRepository wrongQuestionRepository,
            QuestionBankSettingRepository settingRepository,
            QuestionRepository questionRepository,
            ClassificationService classificationService,
            OperationLogService operationLogService,
            ObjectMapper objectMapper) {
        this.answerRecordRepository = answerRecordRepository;
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.settingRepository = settingRepository;
        this.questionRepository = questionRepository;
        this.classificationService = classificationService;
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<QuestionBankDtos.PracticeQuestion> generatePractice(
            QuestionBankDtos.PracticeRequest request,
            AuthenticatedUser currentUser) {
        String mode = normalizeMode(request.mode());
        int requestedCount = request.count() == null
                ? settingRepository.findByUserId(currentUser.getId())
                        .map(QuestionBankSetting::getPracticeCount).orElse(10)
                : request.count();
        List<Question> candidates = new ArrayList<>(
                questionRepository.findAllByCreatedByAndDeletedFalseAndStatus(
                        currentUser.getId(), QuestionStatus.PUBLISHED));

        if (request.categoryId() != null) {
            Set<Long> categoryIds = classificationService.getOwnedCategoryAndDescendantIds(
                    request.categoryId(), currentUser.getId());
            candidates.removeIf(question -> !categoryIds.contains(question.getCategoryId()));
        }
        if (request.knowledgePoint() != null && !request.knowledgePoint().isBlank()) {
            String knowledgePoint = request.knowledgePoint().trim();
            candidates.removeIf(question -> question.getKnowledgePoint() == null
                    || !question.getKnowledgePoint().equalsIgnoreCase(knowledgePoint));
        }

        if ("wrong".equals(mode)) {
            Set<Long> wrongIds = wrongQuestionRepository
                    .findAllByUserIdAndMasteredFalseOrderByWrongCountDescLastWrongAtDesc(currentUser.getId())
                    .stream().map(WrongQuestion::getQuestionId).collect(java.util.stream.Collectors.toSet());
            candidates.removeIf(question -> !wrongIds.contains(question.getId()));
        } else if ("weak".equals(mode) && (request.knowledgePoint() == null || request.knowledgePoint().isBlank())) {
            List<QuestionBankDtos.WeakPoint> weakPoints =
                    buildWeakPoints(answerRecordRepository.findAllByUserIdOrderByAnsweredAtDesc(currentUser.getId()));
            if (!weakPoints.isEmpty()) {
                String weakest = weakPoints.get(0).name();
                candidates.removeIf(question -> !weakest.equals(questionKnowledgeLabel(question)));
            }
        } else if ("challenge".equals(mode)) {
            candidates.removeIf(question -> question.getDifficulty() < 4);
            candidates.sort(Comparator.comparing(Question::getDifficulty).reversed());
        } else if ("chapter".equals(mode)) {
            candidates.sort(Comparator
                    .comparing((Question question) -> nullSafe(question.getSubject()))
                    .thenComparing(question -> nullSafe(question.getKnowledgePoint()))
                    .thenComparing(Question::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        } else {
            Collections.shuffle(candidates);
        }

        if (candidates.isEmpty()) {
            throw new BusinessException(practiceEmptyMessage(mode));
        }
        return candidates.stream()
                .limit(requestedCount)
                .map(this::toPracticeQuestion)
                .toList();
    }

    @Transactional
    public QuestionBankDtos.AnswerResult submitAnswer(
            QuestionBankDtos.AnswerRequest request,
            AuthenticatedUser currentUser) {
        String mode = normalizeMode(request.mode());
        Question question = questionRepository.findByIdAndCreatedBy(request.questionId(), currentUser.getId())
                .orElseThrow(() -> new BusinessException("题目不存在或无权访问"));
        if (Boolean.TRUE.equals(question.getDeleted()) || question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new BusinessException("该题目当前不可练习");
        }

        String userAnswer = normalizeUserAnswer(request.userAnswer());
        boolean correct = request.selfCorrect() != null
                ? Boolean.TRUE.equals(request.selfCorrect())
                : answersEqual(question.getQuestionType(), question.getCorrectAnswer(), userAnswer);
        AnswerRecord record = new AnswerRecord();
        record.setUserId(currentUser.getId());
        record.setQuestionId(question.getId());
        record.setUserAnswer(userAnswer);
        record.setCorrect(correct);
        record.setPracticeMode(mode);
        answerRecordRepository.save(record);

        WrongQuestion wrongQuestion = wrongQuestionRepository
                .findByUserIdAndQuestionId(currentUser.getId(), question.getId())
                .orElse(null);
        if (!correct) {
            if (wrongQuestion == null) {
                wrongQuestion = new WrongQuestion();
                wrongQuestion.setUserId(currentUser.getId());
                wrongQuestion.setQuestionId(question.getId());
                wrongQuestion.setWrongCount(1);
            } else {
                wrongQuestion.setWrongCount(wrongQuestion.getWrongCount() + 1);
            }
            wrongQuestion.setMastered(false);
            wrongQuestion.setLastWrongAt(LocalDateTime.now());
            wrongQuestionRepository.save(wrongQuestion);
        } else if (wrongQuestion != null) {
            wrongQuestion.setLastReviewedAt(LocalDateTime.now());
            if ("wrong".equals(mode)) {
                wrongQuestion.setMastered(true);
            }
            wrongQuestionRepository.save(wrongQuestion);
        }

        int wrongCount = wrongQuestion == null ? 0 : wrongQuestion.getWrongCount();
        boolean mastered = wrongQuestion != null && Boolean.TRUE.equals(wrongQuestion.getMastered());
        return new QuestionBankDtos.AnswerResult(
                correct,
                question.getCorrectAnswer(),
                question.getAnalysis(),
                wrongCount,
                mastered);
    }

    @Transactional(readOnly = true)
    public QuestionBankDtos.Dashboard getDashboard(AuthenticatedUser currentUser) {
        Long userId = currentUser.getId();
        List<AnswerRecord> answers = answerRecordRepository.findAllByUserIdOrderByAnsweredAtDesc(userId);
        Map<Long, Question> questions = questionMap(answers);
        LocalDate today = LocalDate.now();
        List<AnswerRecord> todayAnswers = answers.stream()
                .filter(answer -> answer.getAnsweredAt().toLocalDate().equals(today))
                .toList();
        int goal = settingRepository.findByUserId(userId)
                .map(QuestionBankSetting::getPracticeCount).orElse(10);
        long activeWrong = wrongQuestionRepository.countByUserIdAndMasteredFalse(userId);

        List<QuestionBankDtos.RecentAttempt> recent = answers.stream()
                .filter(answer -> questions.containsKey(answer.getQuestionId()))
                .limit(5)
                .map(answer -> {
                    Question question = questions.get(answer.getQuestionId());
                    return new QuestionBankDtos.RecentAttempt(
                            question.getId(),
                            summarize(question.getContent()),
                            question.getSubject(),
                            Boolean.TRUE.equals(answer.getCorrect()),
                            answer.getPracticeMode(),
                            answer.getAnsweredAt());
                })
                .toList();
        long wrongReviewCount = todayAnswers.stream()
                .filter(answer -> "wrong".equals(answer.getPracticeMode())).count();
        int progress = goal == 0 ? 0 : (int) Math.min(100, Math.round(todayAnswers.size() * 100.0 / goal));
        return new QuestionBankDtos.Dashboard(
                calculateStreak(answers),
                recent,
                new QuestionBankDtos.TodaySummary(
                        todayAnswers.size(),
                        wrongReviewCount,
                        activeWrong,
                        todayAnswers.size() * 2L,
                        goal,
                        progress),
                buildWeakPoints(answers));
    }

    @Transactional(readOnly = true)
    public QuestionBankDtos.Analytics getAnalytics(AuthenticatedUser currentUser) {
        Long userId = currentUser.getId();
        List<AnswerRecord> answers = answerRecordRepository.findAllByUserIdOrderByAnsweredAtDesc(userId);
        Map<Long, Question> questions = questionMap(answers);
        long correctCount = answers.stream().filter(answer -> Boolean.TRUE.equals(answer.getCorrect())).count();
        int accuracy = answers.isEmpty() ? 0 : (int) Math.round(correctCount * 100.0 / answers.size());
        long answeredQuestions = answers.stream().map(AnswerRecord::getQuestionId).distinct().count();

        List<QuestionBankDtos.TrendPoint> trend = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = LocalDate.now().minusDays(offset);
            List<AnswerRecord> daily = answers.stream()
                    .filter(answer -> answer.getAnsweredAt().toLocalDate().equals(date))
                    .toList();
            long dailyCorrect = daily.stream().filter(answer -> Boolean.TRUE.equals(answer.getCorrect())).count();
            int dailyAccuracy = daily.isEmpty() ? 0 : (int) Math.round(dailyCorrect * 100.0 / daily.size());
            trend.add(new QuestionBankDtos.TrendPoint(date, daily.size(), dailyAccuracy));
        }

        Map<QuestionType, AccuracyAccumulator> typeGroups = new LinkedHashMap<>();
        for (AnswerRecord answer : answers) {
            Question question = questions.get(answer.getQuestionId());
            if (question == null) continue;
            typeGroups.computeIfAbsent(question.getQuestionType(), ignored -> new AccuracyAccumulator())
                    .add(Boolean.TRUE.equals(answer.getCorrect()));
        }
        List<QuestionBankDtos.TypeAccuracy> typeAccuracy = typeGroups.entrySet().stream()
                .map(entry -> new QuestionBankDtos.TypeAccuracy(
                        entry.getKey(),
                        entry.getValue().attempts,
                        entry.getValue().accuracy()))
                .toList();

        return new QuestionBankDtos.Analytics(
                new QuestionBankDtos.AnalyticsSummary(
                        questionRepository.countByCreatedByAndDeletedFalse(userId),
                        answeredQuestions,
                        accuracy,
                        wrongQuestionRepository.countByUserIdAndMasteredFalse(userId),
                        answers.size() * 2L),
                trend,
                typeAccuracy,
                buildWeakPoints(answers));
    }

    @Transactional
    public QuestionBankDtos.SettingsResponse getSettings(AuthenticatedUser currentUser) {
        return toSettingsResponse(getOrCreateSettings(currentUser.getId()));
    }

    @Transactional
    public QuestionBankDtos.SettingsResponse updateSettings(
            QuestionBankDtos.SettingsRequest request,
            AuthenticatedUser currentUser) {
        QuestionBankSetting setting = getOrCreateSettings(currentUser.getId());
        setting.setBankName(request.bankName().trim());
        setting.setDescription(normalizeText(request.description()));
        setting.setMemberEdit(request.memberEdit());
        setting.setMemberExport(request.memberExport());
        setting.setReviewRequired(request.reviewRequired());
        setting.setPracticeCount(request.practiceCount());
        setting.setDefaultDifficulty(request.difficulty().trim());
        setting.setSortMode(request.sort().trim());
        setting.setShowAnswer(request.showAnswer().trim());
        QuestionBankSetting saved = settingRepository.save(setting);
        operationLogService.record(currentUser.getId(), "QUESTION_BANK_SETTINGS", "更新题库设置");
        return toSettingsResponse(saved);
    }

    private List<QuestionBankDtos.WeakPoint> buildWeakPoints(List<AnswerRecord> answers) {
        Map<Long, Question> questions = questionMap(answers);
        Map<String, AccuracyAccumulator> groups = new HashMap<>();
        for (AnswerRecord answer : answers) {
            Question question = questions.get(answer.getQuestionId());
            if (question == null) continue;
            groups.computeIfAbsent(questionKnowledgeLabel(question), ignored -> new AccuracyAccumulator())
                    .add(Boolean.TRUE.equals(answer.getCorrect()));
        }
        return groups.entrySet().stream()
                .map(entry -> new QuestionBankDtos.WeakPoint(
                        entry.getKey(),
                        entry.getValue().attempts,
                        entry.getValue().attempts - entry.getValue().correct,
                        entry.getValue().accuracy()))
                .sorted(Comparator.comparing(QuestionBankDtos.WeakPoint::accuracy)
                        .thenComparing(QuestionBankDtos.WeakPoint::wrongCount, Comparator.reverseOrder()))
                .limit(5)
                .toList();
    }

    private Map<Long, Question> questionMap(List<AnswerRecord> answers) {
        Set<Long> ids = answers.stream().map(AnswerRecord::getQuestionId).collect(java.util.stream.Collectors.toSet());
        Map<Long, Question> result = new HashMap<>();
        if (!ids.isEmpty()) {
            questionRepository.findAllById(ids).forEach(question -> result.put(question.getId(), question));
        }
        return result;
    }

    private QuestionBankSetting getOrCreateSettings(Long userId) {
        return settingRepository.findByUserId(userId).orElseGet(() -> {
            QuestionBankSetting setting = new QuestionBankSetting();
            setting.setUserId(userId);
            setting.setBankName("我的题库");
            setting.setDescription("管理自己的题目并进行针对性练习");
            return settingRepository.save(setting);
        });
    }

    private QuestionBankDtos.SettingsResponse toSettingsResponse(QuestionBankSetting setting) {
        return new QuestionBankDtos.SettingsResponse(
                setting.getBankName(),
                setting.getDescription(),
                Boolean.TRUE.equals(setting.getMemberEdit()),
                Boolean.TRUE.equals(setting.getMemberExport()),
                Boolean.TRUE.equals(setting.getReviewRequired()),
                setting.getPracticeCount(),
                setting.getDefaultDifficulty(),
                setting.getSortMode(),
                setting.getShowAnswer(),
                setting.getCreatedAt(),
                setting.getUpdatedAt());
    }

    private QuestionBankDtos.PracticeQuestion toPracticeQuestion(Question question) {
        return new QuestionBankDtos.PracticeQuestion(
                question.getId(),
                question.getContent(),
                question.getQuestionType(),
                readOptions(question.getOptionsJson()),
                question.getDifficulty(),
                question.getSubject(),
                question.getKnowledgePoint(),
                question.getCorrectAnswer(),
                question.getAnalysis());
    }

    private List<String> readOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private boolean answersEqual(QuestionType type, String expected, String actual) {
        if (type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE) {
            return normalizeChoiceAnswer(expected).equals(normalizeChoiceAnswer(actual));
        }
        return normalizePlainAnswer(expected).equals(normalizePlainAnswer(actual));
    }

    private String normalizeUserAnswer(String value) {
        return value == null || value.isBlank() ? "未填写" : value.trim();
    }

    private String normalizeChoiceAnswer(String value) {
        if (value == null) return "";
        return String.join(",", java.util.Arrays.stream(value.toUpperCase(Locale.ROOT)
                        .replaceAll("[，；;]", ",").split("[,\\s]+"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .sorted()
                .toList());
    }

    private String normalizePlainAnswer(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String normalizeMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        if (!PRACTICE_MODES.contains(normalized)) {
            throw new BusinessException("不支持的练习模式");
        }
        return normalized;
    }

    private String practiceEmptyMessage(String mode) {
        return switch (mode) {
            case "wrong" -> "错题本里还没有待复习题目";
            case "weak" -> "暂无可生成的薄弱知识点练习";
            case "challenge" -> "暂无已发布的高难度题目";
            default -> "暂无符合条件的已发布题目";
        };
    }

    private int calculateStreak(List<AnswerRecord> answers) {
        Set<LocalDate> dates = answers.stream()
                .map(answer -> answer.getAnsweredAt().toLocalDate())
                .collect(java.util.stream.Collectors.toSet());
        int streak = 0;
        LocalDate cursor = LocalDate.now();
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private String questionKnowledgeLabel(Question question) {
        if (question.getKnowledgePoint() != null && !question.getKnowledgePoint().isBlank()) {
            return question.getKnowledgePoint();
        }
        if (question.getSubject() != null && !question.getSubject().isBlank()) {
            return question.getSubject();
        }
        return "未分类";
    }

    private String summarize(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 42 ? normalized : normalized.substring(0, 42) + "...";
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static class AccuracyAccumulator {
        private long attempts;
        private long correct;

        void add(boolean isCorrect) {
            attempts++;
            if (isCorrect) correct++;
        }

        int accuracy() {
            return attempts == 0 ? 0 : (int) Math.round(correct * 100.0 / attempts);
        }
    }
}
