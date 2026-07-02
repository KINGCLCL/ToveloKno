package backend.backend.service;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUserContext;
import backend.backend.entity.OperationLog;
import backend.backend.repository.OperationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志业务服务。
 *
 * 业务模块完成关键动作后调用 record 方法即可写入 operation_log 表。
 * 日志写入失败时会跟随当前事务回滚，保证业务数据和日志一致。
 */
@Service
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    public OperationLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    /**
     * 记录当前登录用户的操作。
     *
     * 适合已经经过登录拦截器的业务接口调用。
     */
    @Transactional
    public void record(String operationType, String operationContent) {
        AuthenticatedUser currentUser = CurrentUserContext.get();
        record(currentUser.getId(), operationType, operationContent);
    }

    /**
     * 显式指定用户 id 记录操作。
     *
     * 适合注册、系统任务等还没有当前登录上下文的场景。
     */
    @Transactional
    public void record(Long userId, String operationType, String operationContent) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType(operationType);
        log.setOperationContent(limitLength(operationContent, 500));
        log.setIpAddress(resolveClientIp());
        operationLogRepository.save(log);
    }

    /**
     * 获取客户端 IP。
     *
     * 如果项目以后部署在 Nginx 等代理后面，优先读取 X-Forwarded-For。
     */
    private String resolveClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // 数据库字段长度是 500，这里提前截断，避免长内容导致写库失败。
    private String limitLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
