package backend.backend.studylink;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.auth.CurrentUser;
import backend.backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study-links")
public class StudyLinkOverviewController {

    private final StudyLinkOverviewService studyLinkOverviewService;

    public StudyLinkOverviewController(StudyLinkOverviewService studyLinkOverviewService) {
        this.studyLinkOverviewService = studyLinkOverviewService;
    }

    @GetMapping("/overview")
    public ApiResponse<StudyLinkOverviewResponse> getOverview(@CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.success("查询成功", studyLinkOverviewService.getOverview(currentUser.getId()));
    }
}
