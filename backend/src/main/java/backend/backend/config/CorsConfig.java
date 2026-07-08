package backend.backend.config;

import backend.backend.auth.AuthInterceptor;
import backend.backend.auth.CurrentUserArgumentResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Web 层公共配置。
 *
 * 这里集中配置跨域、登录拦截器、Controller 参数解析器。
 * 后续业务模块只要路径放在 /api/** 下，就会自动套用这些基础能力。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    private final String[] allowedOrigins;
    private final Path profileUploadDir;
    private final Path resourceUploadDir;
    private final Path bannerUploadDir;

    public CorsConfig(
            AuthInterceptor authInterceptor,
            CurrentUserArgumentResolver currentUserArgumentResolver,
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String allowedOrigins,
            @Value("${app.upload.profile-dir:uploads/profile}") String profileUploadDir,
            @Value("${app.upload.resource-dir:uploads/resources}") String resourceUploadDir,
            @Value("${app.upload.banner-dir:uploads/banners}") String bannerUploadDir) {
        this.authInterceptor = authInterceptor;
        this.currentUserArgumentResolver = currentUserArgumentResolver;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
        this.profileUploadDir = Path.of(profileUploadDir).toAbsolutePath().normalize();
        this.resourceUploadDir = Path.of(resourceUploadDir).toAbsolutePath().normalize();
        this.bannerUploadDir = Path.of(bannerUploadDir).toAbsolutePath().normalize();
    }

    /**
     * 允许本地 Vue 开发服务访问后端接口。
     *
     * exposedHeaders 目前预留给前端读取 Authorization 等响应头。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization");
        registry.addMapping("/uploads/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/profile/**")
                .addResourceLocations(profileUploadDir.toUri().toString() + "/");
        registry.addResourceHandler("/uploads/resources/**")
                .addResourceLocations(resourceUploadDir.toUri().toString() + "/");
        registry.addResourceHandler("/uploads/banners/**")
                .addResourceLocations(bannerUploadDir.toUri().toString() + "/");
    }

    /**
     * 将登录拦截器挂到所有 /api/** 接口上。
     *
     * 哪些接口放行由 AuthInterceptor 内部决定。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**");
    }

    /**
     * 注册 @CurrentUser 参数解析器。
     *
     * Controller 方法里写 @CurrentUser AuthenticatedUser currentUser 就能拿到当前用户。
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
