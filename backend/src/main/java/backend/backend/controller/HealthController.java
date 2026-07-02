package backend.backend.controller;

import backend.backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success("服务运行中", Map.of(
                "status", "UP",
                "time", LocalDateTime.now()
        ));
    }
}
