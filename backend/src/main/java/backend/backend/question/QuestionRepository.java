package backend.backend.question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 题目数据访问层，支持基础 CRUD、动态筛选与统计。
 */
public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    Optional<Question> findByIdAndCreatedBy(Long id, Long createdBy);

    List<Question> findAllByIdInAndCreatedBy(Collection<Long> ids, Long createdBy);

    long countByCreatedByAndDeletedFalse(Long createdBy);

    long countByCreatedByAndDeletedFalseAndStatus(Long createdBy, QuestionStatus status);

    long countByCreatedByAndDeletedTrue(Long createdBy);

    long countByCreatedByAndDeletedFalseAndCategoryIdIsNotNull(Long createdBy);

    long countByCreatedByAndDeletedFalseAndCategoryIdIsNull(Long createdBy);

    long countByCreatedByAndDeletedFalseAndSourceType(Long createdBy, String sourceType);

    List<Question> findTop5ByCreatedByAndDeletedFalseAndSourceTypeOrderByUpdatedAtDesc(Long createdBy, String sourceType);

    List<Question> findAllByCreatedByAndDeletedFalseAndCategoryId(Long createdBy, Long categoryId);

    List<Question> findAllByCreatedByAndDeletedFalseAndStatus(Long createdBy, QuestionStatus status);

    @Query("""
            select q.categoryId, count(q.id)
            from Question q
            where q.createdBy = :createdBy and q.deleted = false and q.categoryId is not null
            group by q.categoryId
            """)
    List<Object[]> countQuestionsByCategory(@Param("createdBy") Long createdBy);

    @Query("""
            select q.knowledgePoint, q.subject, count(q.id),
                   sum(case when q.status = :publishedStatus then 1 else 0 end),
                   max(q.updatedAt)
            from Question q
            where q.createdBy = :createdBy
              and q.deleted = false
              and q.knowledgePoint is not null
              and trim(q.knowledgePoint) <> ''
            group by q.knowledgePoint, q.subject
            order by count(q.id) desc, q.knowledgePoint asc
            """)
    List<Object[]> aggregateKnowledgePoints(
            @Param("createdBy") Long createdBy,
            @Param("publishedStatus") QuestionStatus publishedStatus);

    @Query("""
            select q.difficulty, count(q.id)
            from Question q
            where q.createdBy = :createdBy and q.deleted = false
            group by q.difficulty
            order by q.difficulty asc
            """)
    List<Object[]> aggregateDifficulties(@Param("createdBy") Long createdBy);

    @Query("""
            select q
            from Question q
            where q.createdBy = :createdBy
              and q.deleted = false
              and lower(q.knowledgePoint) = lower(:knowledgePoint)
              and ((:subject is null and q.subject is null) or lower(q.subject) = lower(:subject))
            """)
    List<Question> findByKnowledgePoint(
            @Param("createdBy") Long createdBy,
            @Param("knowledgePoint") String knowledgePoint,
            @Param("subject") String subject);
}
