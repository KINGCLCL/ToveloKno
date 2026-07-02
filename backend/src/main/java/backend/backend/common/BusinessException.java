package backend.backend.common;

/**
 * 通用业务异常。
 *
 * 当业务规则不满足时可以抛出这个异常，例如“资源标题已存在”。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
