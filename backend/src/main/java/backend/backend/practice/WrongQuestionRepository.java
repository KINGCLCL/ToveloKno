package backend.backend.practice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WrongQuestionRepository extends JpaRepository<WrongQuestion, Long> {
    Optional<WrongQuestion> findByUserIdAndQuestionId(Long userId, Long questionId);
    List<WrongQuestion> findAllByUserIdAndMasteredFalseOrderByWrongCountDescLastWrongAtDesc(Long userId);
    long countByUserIdAndMasteredFalse(Long userId);
}
