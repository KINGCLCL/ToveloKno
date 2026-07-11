package backend.backend.studyforum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForumThreadFavoriteRepository extends JpaRepository<ForumThreadFavorite, Long> {
    List<ForumThreadFavorite> findAllByUserId(Long userId);

    Optional<ForumThreadFavorite> findByUserIdAndThreadId(Long userId, Long threadId);
}
