package backend.backend.wrongquestion.repository;

import backend.backend.wrongquestion.entity.WrongQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 错题本数据访问层。
 */
@Repository("wrongQuestionBookRepository")
public interface WrongQuestionRepository extends JpaRepository<WrongQuestion, Long> {

    Optional<WrongQuestion> findByIdAndUserId(Long id, Long userId);

    @Query(value = """
            SELECT
                w.id AS id,
                w.question_id AS questionId,
                q.content AS content,
                q.question_type AS questionType,
                q.correct_answer AS correctAnswer,
                q.analysis AS analysis,
                q.difficulty AS difficulty,
                q.source_type AS sourceType,
                q.source_resource_id AS sourceResourceId,
                q.source_resource_name AS sourceResourceName,
                q.source_page AS sourcePage,
                q.source_excerpt AS sourceExcerpt,
                w.wrong_count AS wrongCount,
                w.mastered AS mastered,
                w.last_wrong_at AS lastWrongAt,
                w.last_reviewed_at AS lastReviewedAt
            FROM wrong_question w
            JOIN question q ON w.question_id = q.id
            WHERE w.user_id = :userId
              AND (:mastered IS NULL OR w.mastered = :mastered)
            ORDER BY w.last_wrong_at DESC
            """, nativeQuery = true)
    List<WrongQuestionView> findWrongQuestionList(
            @Param("userId") Long userId,
            @Param("mastered") Integer mastered
    );

    interface WrongQuestionView {
        Long getId();

        Long getQuestionId();

        String getContent();

        String getQuestionType();

        String getCorrectAnswer();

        String getAnalysis();

        Integer getDifficulty();

        String getSourceType();

        Long getSourceResourceId();

        String getSourceResourceName();

        Integer getSourcePage();

        String getSourceExcerpt();

        Integer getWrongCount();

        Integer getMastered();

        LocalDateTime getLastWrongAt();

        LocalDateTime getLastReviewedAt();
    }
}
