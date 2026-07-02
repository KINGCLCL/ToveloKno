package backend.backend.controller;

import backend.backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查接口。
 *
 * 前端、本地联调或部署环境可以用这个接口快速确认后端服务是否启动成功。
 */
@RestController
public class HealthController {

    /**
     * 返回服务运行状态。
     *
     * 这个接口在 AuthInterceptor 里被配置为公开接口，不需要登录。
     */
    @GetMapping("/api/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success("服务运行中", Map.of(
                "status", "UP",
                "time", LocalDateTime.now()
        ));
    }
}
