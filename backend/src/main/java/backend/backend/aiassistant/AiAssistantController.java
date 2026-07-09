package backend.backend.aiassistant;

import backend.backend.aiassistant.AiAssistantDtos.AiAnalysisRequest;
import backend.backend.aiassistant.AiAssistantDtos.AiAnalysisResponse;
import backend.backend.aiassistant.AiAssistantDtos.AiAssistantStatusResponse;
import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-assistant")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @GetMapping("/status")
    public ApiResponse<AiAssistantStatusResponse> status() {
        return ApiResponse.success("查询成功", aiAssistantService.status());
    }

    @PostMapping("/analyze")
    public ApiResponse<AiAnalysisResponse> analyze(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody AiAnalysisRequest request) {
        return ApiResponse.success("AI 分析完成", aiAssistantService.analyze(request, currentUser));
    }
}
