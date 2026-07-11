package backend.backend.studyforum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ForumReplyRepository extends JpaRepository<ForumReply, Long> {
    List<ForumReply> findAllByThreadIdInOrderByCreatedAtAsc(Collection<Long> threadIds);

    List<ForumReply> findAllByThreadIdOrderByCreatedAtAsc(Long threadId);
}
