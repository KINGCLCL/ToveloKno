package backend.backend.config;

import backend.backend.auth.AuthInterceptor;
import backend.backend.auth.CurrentUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

    public CorsConfig(AuthInterceptor authInterceptor, CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.authInterceptor = authInterceptor;
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    /**
     * 允许本地 Vue 开发服务访问后端接口。
     *
     * exposedHeaders 目前预留给前端读取 Authorization 等响应头。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization");
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
