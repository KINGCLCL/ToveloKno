package backend.backend.classification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findAllByCreatedByOrderByNameAsc(Long createdBy);

    Optional<Tag> findByIdAndCreatedBy(Long id, Long createdBy);

    boolean existsByCreatedByAndNameIgnoreCase(Long createdBy, String name);
}
