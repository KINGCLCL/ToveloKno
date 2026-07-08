package backend.backend.studyplan.repository;

import backend.backend.studyplan.entity.StudyPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 学习计划数据访问层。
 */
public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

    /**
     * 根据 id 和 userId 查询单条计划，确保数据归属校验。
     */
    Optional<StudyPlan> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    List<StudyPlan> findTop5ByUserIdOrderByPlanDateDescCreatedAtDesc(Long userId);

    /**
     * 分页查询当前用户的学习计划，支持按状态和日期筛选。
     */
    @Query("""
            SELECT s FROM StudyPlan s
            WHERE s.userId = :userId
              AND (:status IS NULL OR s.status = :status)
              AND (:planDate IS NULL OR s.planDate = :planDate)
            ORDER BY s.planDate DESC, s.createdAt DESC
            """)
    Page<StudyPlan> findStudyPlanPage(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("planDate") LocalDate planDate,
            Pageable pageable
    );
}
