package backend.backend.common;

/**
 * 统一接口返回格式。
 *
 * 前端调用任何接口时，都可以按 success、message、data 这三个字段处理结果，
 * 这样后面写 Vue 页面时不用为每个接口单独猜返回结构。
 */
public class ApiResponse<T> {

    // true 表示业务处理成功，false 表示业务处理失败。
    private boolean success;

    // 给前端展示或调试用的提示信息。
    private String message;

    // 真正返回的数据内容，例如用户信息、资料列表、题目列表等。
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // 成功响应的快捷创建方法，减少 Controller 中的重复代码。
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    // 失败响应的快捷创建方法，失败时通常不需要返回 data。
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
