package backend.backend.studyforum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForumThreadLikeRepository extends JpaRepository<ForumThreadLike, Long> {
    Optional<ForumThreadLike> findByUserIdAndThreadId(Long userId, Long threadId);
}
