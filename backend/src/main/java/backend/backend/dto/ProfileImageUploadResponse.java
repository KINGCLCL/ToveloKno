package backend.backend.dto;

/**
 * 个人主页图片上传响应。
 */
public class ProfileImageUploadResponse {

    private String url;
    private UserResponse user;

    public ProfileImageUploadResponse(String url, UserResponse user) {
        this.url = url;
        this.user = user;
    }

    public String getUrl() {
        return url;
    }

    public UserResponse getUser() {
        return user;
    }
}
