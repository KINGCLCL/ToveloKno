package backend.backend.learningresource;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long>,
        JpaSpecificationExecutor<LearningResource> {

    Optional<LearningResource> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndFavoriteTrue(Long userId);
}
