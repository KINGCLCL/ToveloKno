package backend.backend.repository;

import backend.backend.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
}
