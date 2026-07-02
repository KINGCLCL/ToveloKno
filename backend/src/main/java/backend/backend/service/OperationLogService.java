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

@Service
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    public OperationLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    @Transactional
    public void record(String operationType, String operationContent) {
        AuthenticatedUser currentUser = CurrentUserContext.get();
        record(currentUser.getId(), operationType, operationContent);
    }

    @Transactional
    public void record(Long userId, String operationType, String operationContent) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType(operationType);
        log.setOperationContent(limitLength(operationContent, 500));
        log.setIpAddress(resolveClientIp());
        operationLogRepository.save(log);
    }

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

    private String limitLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
