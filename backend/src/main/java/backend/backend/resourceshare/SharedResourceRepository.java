package backend.backend.resourceshare;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SharedResourceRepository extends JpaRepository<SharedResource, Long> {
    List<SharedResource> findAllByOrderByUpdatedAtDesc();
}
