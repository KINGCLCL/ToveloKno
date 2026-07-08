package backend.backend.studylink;

import backend.backend.learningresource.LearningResourceRepository;
import backend.backend.question.Question;
import backend.backend.question.QuestionRepository;
import backend.backend.studyplan.entity.StudyPlan;
import backend.backend.studyplan.repository.StudyPlanRepository;
import backend.backend.wrongquestion.repository.WrongQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyLinkOverviewService {

    private static final String RESOURCE_EXCERPT = "RESOURCE_EXCERPT";

    private final LearningResourceRepository learningResourceRepository;
    private final QuestionRepository questionRepository;
    private final WrongQuestionRepository wrongQuestionRepository;
    private final StudyPlanRepository studyPlanRepository;

    public StudyLinkOverviewService(
            LearningResourceRepository learningResourceRepository,
            QuestionRepository questionRepository,
            WrongQuestionRepository wrongQuestionRepository,
            StudyPlanRepository studyPlanRepository) {
        this.learningResourceRepository = learningResourceRepository;
        this.questionRepository = questionRepository;
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.studyPlanRepository = studyPlanRepository;
    }

    @Transactional(readOnly = true)
    public StudyLinkOverviewResponse getOverview(Long userId) {
        long resourceCount = learningResourceRepository.countByUserId(userId);
        long questionCount = questionRepository.countByCreatedByAndDeletedFalse(userId);
        long resourceQuestionCount = questionRepository.countByCreatedByAndDeletedFalseAndSourceType(
                userId,
                RESOURCE_EXCERPT);
        long activeWrongCount = wrongQuestionRepository.findWrongQuestionList(userId, 0).size();
        long pendingPlanCount = studyPlanRepository.countByUserIdAndStatus(userId, "pending");

        return new StudyLinkOverviewResponse(
                resourceCount,
                questionCount,
                resourceQuestionCount,
                activeWrongCount,
                pendingPlanCount,
                questionRepository
                        .findTop5ByCreatedByAndDeletedFalseAndSourceTypeOrderByUpdatedAtDesc(userId, RESOURCE_EXCERPT)
                        .stream()
                        .map(this::toRecentQuestion)
                        .toList(),
                studyPlanRepository
                        .findTop5ByUserIdOrderByPlanDateDescCreatedAtDesc(userId)
                        .stream()
                        .map(this::toRecentPlan)
                        .toList()
        );
    }

    private StudyLinkOverviewResponse.RecentQuestion toRecentQuestion(Question question) {
        return new StudyLinkOverviewResponse.RecentQuestion(
                question.getId(),
                summarize(question.getContent()),
                question.getSourceResourceName(),
                question.getSourcePage(),
                question.getUpdatedAt()
        );
    }

    private StudyLinkOverviewResponse.RecentPlan toRecentPlan(StudyPlan plan) {
        return new StudyLinkOverviewResponse.RecentPlan(
                plan.getId(),
                plan.getTitle(),
                plan.getTargetType(),
                plan.getTargetId(),
                plan.getTargetTitle(),
                plan.getPlanDate(),
                plan.getStatus()
        );
    }

    private String summarize(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80) + "...";
    }
}
