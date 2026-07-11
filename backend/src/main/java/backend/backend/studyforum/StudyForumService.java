package backend.backend.studyforum;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.common.BusinessException;
import backend.backend.entity.User;
import backend.backend.repository.UserRepository;
import backend.backend.studyforum.StudyForumDtos.ForumReplyRequest;
import backend.backend.studyforum.StudyForumDtos.ForumReplyResponse;
import backend.backend.studyforum.StudyForumDtos.ForumThreadRequest;
import backend.backend.studyforum.StudyForumDtos.ForumThreadResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudyForumService {
    private final ForumThreadRepository threadRepository;
    private final ForumReplyRepository replyRepository;
    private final ForumThreadFavoriteRepository favoriteRepository;
    private final ForumThreadLikeRepository likeRepository;
    private final UserRepository userRepository;

    public StudyForumService(
            ForumThreadRepository threadRepository,
            ForumReplyRepository replyRepository,
            ForumThreadFavoriteRepository favoriteRepository,
            ForumThreadLikeRepository likeRepository,
            UserRepository userRepository) {
        this.threadRepository = threadRepository;
        this.replyRepository = replyRepository;
        this.favoriteRepository = favoriteRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
    }

    public List<ForumThreadResponse> list(AuthenticatedUser currentUser) {
        List<ForumThread> threads = threadRepository.findAllByOrderByLastRepliedAtDescUpdatedAtDesc();
        List<Long> threadIds = threads.stream().map(ForumThread::getId).toList();
        Map<Long, List<ForumReply>> replies = threadIds.isEmpty()
                ? Map.of()
                : replyRepository.findAllByThreadIdInOrderByCreatedAtAsc(threadIds).stream()
                .collect(Collectors.groupingBy(ForumReply::getThreadId));
        Set<Long> favoriteIds = favoriteRepository.findAllByUserId(currentUser.getId()).stream()
                .map(ForumThreadFavorite::getThreadId)
                .collect(Collectors.toSet());
        Set<Long> likedIds = threads.stream()
                .filter(thread -> likeRepository.findByUserIdAndThreadId(currentUser.getId(), thread.getId()).isPresent())
                .map(ForumThread::getId)
                .collect(Collectors.toSet());
        Map<Long, User> users = loadUsers(threads, replies.values().stream().flatMap(Collection::stream).toList());
        return threads.stream()
                .map(thread -> toResponse(thread, replies.getOrDefault(thread.getId(), List.of()), users, favoriteIds.contains(thread.getId()), likedIds.contains(thread.getId())))
                .toList();
    }

    public ForumThreadResponse createThread(AuthenticatedUser currentUser, ForumThreadRequest request) {
        LocalDateTime now = LocalDateTime.now();
        ForumThread thread = new ForumThread();
        thread.setUserId(currentUser.getId());
        thread.setBoardId(clean(request.boardId(), 40));
        thread.setTitle(clean(request.title(), 120));
        thread.setContent(clean(request.content(), 1200));
        thread.setTags(clean(request.tags(), 300));
        thread.setPinned(false);
        thread.setViewCount(1);
        thread.setLikeCount(0);
        thread.setReplyCount(0);
        thread.setLastReplyUserId(currentUser.getId());
        thread.setLastRepliedAt(now);
        thread.setCreatedAt(now);
        thread.setUpdatedAt(now);
        ForumThread saved = threadRepository.save(thread);
        User author = userRepository.findById(currentUser.getId()).orElse(null);
        return toResponse(saved, List.of(), Map.of(currentUser.getId(), author), false, false);
    }

    public ForumThreadResponse viewThread(Long threadId, AuthenticatedUser currentUser) {
        ForumThread thread = findThread(threadId);
        thread.setViewCount(thread.getViewCount() + 1);
        thread.setUpdatedAt(LocalDateTime.now());
        ForumThread saved = threadRepository.save(thread);
        return detail(saved, currentUser);
    }

    public ForumThreadResponse addReply(Long threadId, AuthenticatedUser currentUser, ForumReplyRequest request) {
        ForumThread thread = findThread(threadId);
        LocalDateTime now = LocalDateTime.now();
        ForumReply reply = new ForumReply();
        reply.setThreadId(threadId);
        reply.setUserId(currentUser.getId());
        reply.setContent(clean(request.content(), 800));
        reply.setCreatedAt(now);
        replyRepository.save(reply);
        thread.setReplyCount(thread.getReplyCount() + 1);
        thread.setLastReplyUserId(currentUser.getId());
        thread.setLastRepliedAt(now);
        thread.setUpdatedAt(now);
        ForumThread saved = threadRepository.save(thread);
        return detail(saved, currentUser);
    }

    public ForumThreadResponse toggleFavorite(Long threadId, AuthenticatedUser currentUser) {
        ForumThread thread = findThread(threadId);
        var existing = favoriteRepository.findByUserIdAndThreadId(currentUser.getId(), threadId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
        } else {
            ForumThreadFavorite favorite = new ForumThreadFavorite();
            favorite.setUserId(currentUser.getId());
            favorite.setThreadId(threadId);
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteRepository.save(favorite);
        }
        return detail(thread, currentUser);
    }

    public ForumThreadResponse toggleLike(Long threadId, AuthenticatedUser currentUser) {
        ForumThread thread = findThread(threadId);
        var existing = likeRepository.findByUserIdAndThreadId(currentUser.getId(), threadId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            thread.setLikeCount(Math.max(0, thread.getLikeCount() - 1));
        } else {
            ForumThreadLike like = new ForumThreadLike();
            like.setUserId(currentUser.getId());
            like.setThreadId(threadId);
            like.setCreatedAt(LocalDateTime.now());
            likeRepository.save(like);
            thread.setLikeCount(thread.getLikeCount() + 1);
        }
        thread.setUpdatedAt(LocalDateTime.now());
        ForumThread saved = threadRepository.save(thread);
        return detail(saved, currentUser);
    }

    private ForumThreadResponse detail(ForumThread thread, AuthenticatedUser currentUser) {
        List<ForumReply> replies = replyRepository.findAllByThreadIdOrderByCreatedAtAsc(thread.getId());
        Map<Long, User> users = loadUsers(List.of(thread), replies);
        boolean favorite = favoriteRepository.findByUserIdAndThreadId(currentUser.getId(), thread.getId()).isPresent();
        boolean liked = likeRepository.findByUserIdAndThreadId(currentUser.getId(), thread.getId()).isPresent();
        return toResponse(thread, replies, users, favorite, liked);
    }

    private ForumThread findThread(Long threadId) {
        return threadRepository.findById(threadId)
                .orElseThrow(() -> new BusinessException("主题不存在"));
    }

    private Map<Long, User> loadUsers(List<ForumThread> threads, List<ForumReply> replies) {
        Set<Long> userIds = threads.stream().flatMap(thread -> java.util.stream.Stream.of(thread.getUserId(), thread.getLastReplyUserId()))
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        replies.stream().map(ForumReply::getUserId).forEach(userIds::add);
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private ForumThreadResponse toResponse(ForumThread thread, List<ForumReply> replies, Map<Long, User> users, boolean favorite, boolean liked) {
        List<ForumReplyResponse> replyResponses = replies.stream()
                .map(reply -> new ForumReplyResponse(reply.getId(), reply.getUserId(), displayName(users.get(reply.getUserId())), reply.getContent(), reply.getCreatedAt()))
                .toList();
        return new ForumThreadResponse(
                thread.getId(),
                thread.getUserId(),
                displayName(users.get(thread.getUserId())),
                thread.getBoardId(),
                thread.getTitle(),
                thread.getContent(),
                splitTags(thread.getTags()),
                Boolean.TRUE.equals(thread.getPinned()),
                thread.getViewCount() == null ? 0 : thread.getViewCount(),
                thread.getLikeCount() == null ? 0 : thread.getLikeCount(),
                replyResponses.size(),
                favorite,
                liked,
                displayName(users.get(thread.getLastReplyUserId())),
                thread.getCreatedAt(),
                thread.getUpdatedAt(),
                thread.getLastRepliedAt(),
                replyResponses);
    }

    private String displayName(User user) {
        if (user == null) return "学习用户";
        return user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname() : user.getUsername();
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) return List.of("讨论");
        return Arrays.stream(tags.split("[,，]")).map(String::trim).filter(value -> !value.isBlank()).limit(4).toList();
    }

    private String clean(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
