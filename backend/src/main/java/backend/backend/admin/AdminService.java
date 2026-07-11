package backend.backend.admin;

import backend.backend.admin.AdminDtos.AdminActivity;
import backend.backend.admin.AdminDtos.AdminOverviewResponse;
import backend.backend.admin.AdminDtos.AdminUserResponse;
import backend.backend.auth.AuthenticatedUser;
import backend.backend.common.BusinessException;
import backend.backend.common.ForbiddenException;
import backend.backend.learningresource.LearningResourceRepository;
import backend.backend.question.QuestionRepository;
import backend.backend.resourceshare.SharedResourceRepository;
import backend.backend.studyforum.ForumThreadRepository;
import backend.backend.studyplan.repository.StudyPlanRepository;
import backend.backend.entity.User;
import backend.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final QuestionRepository questionRepository;
    private final SharedResourceRepository sharedResourceRepository;
    private final ForumThreadRepository forumThreadRepository;
    private final StudyPlanRepository studyPlanRepository;

    public AdminService(
            UserRepository userRepository,
            LearningResourceRepository learningResourceRepository,
            QuestionRepository questionRepository,
            SharedResourceRepository sharedResourceRepository,
            ForumThreadRepository forumThreadRepository,
            StudyPlanRepository studyPlanRepository) {
        this.userRepository = userRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.questionRepository = questionRepository;
        this.sharedResourceRepository = sharedResourceRepository;
        this.forumThreadRepository = forumThreadRepository;
        this.studyPlanRepository = studyPlanRepository;
    }

    public AdminOverviewResponse overview(AuthenticatedUser currentUser) {
        requireAdmin(currentUser);
        List<AdminActivity> activities = forumThreadRepository.findAllByOrderByLastRepliedAtDescUpdatedAtDesc().stream()
                .limit(4)
                .map(thread -> new AdminActivity(
                        "FORUM",
                        thread.getTitle(),
                        "论坛 · " + thread.getBoardId() + " · " + thread.getReplyCount() + " 条回复",
                        thread.getLastRepliedAt() == null ? thread.getUpdatedAt() : thread.getLastRepliedAt()))
                .toList();
        return new AdminOverviewResponse(
                userRepository.count(),
                userRepository.countByStatus(1),
                learningResourceRepository.count(),
                questionRepository.countByDeletedFalse(),
                sharedResourceRepository.count(),
                forumThreadRepository.count(),
                studyPlanRepository.count(),
                activities);
    }

    public List<AdminUserResponse> users(AuthenticatedUser currentUser) {
        requireAdmin(currentUser);
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(12)
                .map(this::toResponse)
                .toList();
    }

    public AdminUserResponse updateUserStatus(Long userId, Integer status, AuthenticatedUser currentUser) {
        requireAdmin(currentUser);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("账号状态只能是启用或停用");
        }
        if (currentUser.getId().equals(userId) && status == 0) {
            throw new BusinessException("不能在管理员控制台停用当前账号");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setStatus(status);
        return toResponse(userRepository.save(user));
    }

    private void requireAdmin(AuthenticatedUser currentUser) {
        if (!currentUser.hasRole("ADMIN")) {
            throw new ForbiddenException("仅管理员可访问系统控制台");
        }
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getStatus(),
                user.getRoles().stream().map(role -> role.getRoleName()).sorted().toList(),
                user.getCreatedAt());
    }
}
