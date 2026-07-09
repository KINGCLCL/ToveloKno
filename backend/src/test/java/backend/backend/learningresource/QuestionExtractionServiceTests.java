package backend.backend.learningresource;

import backend.backend.auth.AuthenticatedUser;
import backend.backend.question.QuestionResponse;
import backend.backend.question.QuestionSaveRequest;
import backend.backend.question.QuestionService;
import backend.backend.question.QuestionStatus;
import backend.backend.question.QuestionType;
import backend.backend.service.OperationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionExtractionServiceTests {

    @TempDir
    private Path uploadRoot;

    @Mock
    private LearningResourceRepository learningResourceRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private OperationLogService operationLogService;

    @Test
    void extractsChoiceQuestionFromTextResource() throws Exception {
        Files.writeString(uploadRoot.resolve("sample.txt"), "\uFEFF" + """
                1. 数据库事务的四个特性简称是什么？
                A. BASE
                B. ACID
                C. CRUD
                D. CAP
                答案：B
                解析：事务具有原子性、一致性、隔离性和持久性。
                """, StandardCharsets.UTF_8);
        LearningResource resource = resource();
        AuthenticatedUser currentUser = new AuthenticatedUser(7L, "tester", List.of("USER"));
        when(learningResourceRepository.findByIdAndUserId(3L, 7L)).thenReturn(Optional.of(resource));
        when(questionService.createQuestion(any(QuestionSaveRequest.class), eq(currentUser)))
                .thenReturn(questionResponse());

        QuestionExtractionService service = new QuestionExtractionService(
                learningResourceRepository,
                questionService,
                operationLogService,
                uploadRoot.toString());

        LearningResourceDtos.QuestionExtractionResponse response =
                service.extractAndCreate(3L, currentUser, 10, QuestionStatus.DRAFT);

        ArgumentCaptor<QuestionSaveRequest> captor = ArgumentCaptor.forClass(QuestionSaveRequest.class);
        verify(questionService).createQuestion(captor.capture(), eq(currentUser));
        QuestionSaveRequest request = captor.getValue();
        assertThat(request.getQuestionType()).isEqualTo(QuestionType.SINGLE_CHOICE);
        assertThat(request.getOptions()).containsExactly("A. BASE", "B. ACID", "C. CRUD", "D. CAP");
        assertThat(request.getCorrectAnswer()).isEqualTo("B");
        assertThat(request.getStatus()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(request.getSourceType()).isEqualTo("RESOURCE_AI_EXTRACT");
        assertThat(request.getSourceResourceId()).isEqualTo(3L);
        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.previews()).hasSize(1);
        verify(operationLogService).record(7L, "RESOURCE_AI_EXTRACT_QUESTIONS", "从学习资料自动识别题目：数据库练习.txt，入库 1 题");
    }

    private LearningResource resource() {
        LearningResource resource = new LearningResource();
        resource.setId(3L);
        resource.setUserId(7L);
        resource.setName("数据库练习.txt");
        resource.setType("文档");
        resource.setMimeType("text/plain");
        resource.setOriginalFilename("数据库练习.txt");
        resource.setStoredFilename("sample.txt");
        resource.setFileUrl("/uploads/resources/sample.txt");
        resource.setFileSize(120L);
        return resource;
    }

    private QuestionResponse questionResponse() {
        return new QuestionResponse(
                22L,
                "数据库事务的四个特性简称是什么？",
                QuestionType.SINGLE_CHOICE,
                List.of("A. BASE", "B. ACID", "C. CRUD", "D. CAP"),
                "B",
                "事务具有原子性、一致性、隔离性和持久性。",
                3,
                "学习资料",
                "数据库练习",
                QuestionStatus.DRAFT,
                null,
                "RESOURCE_AI_EXTRACT",
                3L,
                "数据库练习.txt",
                null,
                "source",
                7L,
                false,
                null,
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
