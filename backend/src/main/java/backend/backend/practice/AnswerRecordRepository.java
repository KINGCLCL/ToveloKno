package backend.backend.practice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnswerRecordRepository extends JpaRepository<AnswerRecord, Long> {
    List<AnswerRecord> findAllByUserIdOrderByAnsweredAtDesc(Long userId);
    List<AnswerRecord> findAllByUserIdAndAnsweredAtAfterOrderByAnsweredAtDesc(
            Long userId, LocalDateTime answeredAt);
}
