package backend.backend.repository;

import backend.backend.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 操作日志数据访问层。
 *
 * JpaRepository 已经提供基础新增、删除、分页查询能力；
 * 后续后台日志列表需要筛选时，可以在这里继续补充查询方法。
 */
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
}
