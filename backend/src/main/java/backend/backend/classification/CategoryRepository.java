package backend.backend.classification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByCreatedByOrderBySortOrderAscNameAsc(Long createdBy);

    Optional<Category> findByIdAndCreatedBy(Long id, Long createdBy);

    boolean existsByCreatedByAndParentIdAndNameIgnoreCase(Long createdBy, Long parentId, String name);

    boolean existsByCreatedByAndParentIdIsNullAndNameIgnoreCase(Long createdBy, String name);

    boolean existsByCreatedByAndParentId(Long createdBy, Long parentId);
}
