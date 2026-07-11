package backend.backend.studyforum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumThreadRepository extends JpaRepository<ForumThread, Long> {
    List<ForumThread> findAllByOrderByLastRepliedAtDescUpdatedAtDesc();
}
