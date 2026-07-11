package backend.backend.resourceshare;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SharedResourceFavoriteRepository extends JpaRepository<SharedResourceFavorite, Long> {
    List<SharedResourceFavorite> findAllByUserId(Long userId);

    Optional<SharedResourceFavorite> findByUserIdAndResourceId(Long userId, Long resourceId);

    long countByResourceId(Long resourceId);
}
