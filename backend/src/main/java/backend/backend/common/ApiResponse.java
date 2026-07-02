package backend.backend.common;

/**
 * 后端接口统一返回格式。
 *
 * 前端调用任何接口时，都可以按 success、message、data 三个字段处理结果。
 * 后续业务模块也建议统一返回 ApiResponse，避免不同 Controller 返回结构不一致。
 */
public class ApiResponse<T> {

    // true 表示业务处理成功；false 表示业务处理失败。
    private boolean success;

    // 给前端展示或调试用的提示信息。
    private String message;

    // 真正返回的数据内容，例如用户信息、资源列表、题目列表等。
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应。
     *
     * Controller 中通常直接 return ApiResponse.success("查询成功", data)。
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * 创建失败响应。
     *
     * 业务代码一般不直接调用这个方法，而是抛异常交给 GlobalExceptionHandler 处理。
     */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
