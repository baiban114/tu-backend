package com.tu.backend.taskintegration;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.taskintegration.dto.CreateExternalTaskRequest;
import com.tu.backend.taskintegration.dto.CreateTaskRelationRequest;
import com.tu.backend.taskintegration.dto.ExternalProjectDto;
import com.tu.backend.taskintegration.dto.ExternalProviderDto;
import com.tu.backend.taskintegration.dto.ExternalTaskDto;
import com.tu.backend.taskintegration.dto.IntegrationConnectionDto;
import com.tu.backend.taskintegration.dto.IntegrationConnectionTestResultDto;
import com.tu.backend.taskintegration.dto.MoveExternalTaskRequest;
import com.tu.backend.taskintegration.dto.TaskRelationDto;
import com.tu.backend.taskintegration.dto.UpdateIntegrationConnectionRequest;
import com.tu.backend.taskintegration.dto.UpdateExternalTaskRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/task-integrations")
public class TaskIntegrationController {

    private final TaskIntegrationService taskIntegrationService;

    public TaskIntegrationController(TaskIntegrationService taskIntegrationService) {
        this.taskIntegrationService = taskIntegrationService;
    }

    @GetMapping("/providers")
    public ApiResponse<List<ExternalProviderDto>> providers() {
        return ApiResponse.success(taskIntegrationService.providers());
    }

    @GetMapping("/connection")
    public ApiResponse<IntegrationConnectionDto> connection() {
        return ApiResponse.success(taskIntegrationService.getConnection());
    }

    @GetMapping("/connections")
    public ApiResponse<List<IntegrationConnectionDto>> connections() {
        return ApiResponse.success(taskIntegrationService.listConnections());
    }

    @PostMapping("/connections")
    public ApiResponse<IntegrationConnectionDto> createConnection(@Valid @RequestBody UpdateIntegrationConnectionRequest request) {
        return ApiResponse.success(taskIntegrationService.createConnection(request));
    }

    @PatchMapping("/connection")
    public ApiResponse<IntegrationConnectionDto> updateConnection(@Valid @RequestBody UpdateIntegrationConnectionRequest request) {
        return ApiResponse.success(taskIntegrationService.updateConnection(request));
    }

    @PatchMapping("/connections/{id}")
    public ApiResponse<IntegrationConnectionDto> updateConnection(@PathVariable String id, @Valid @RequestBody UpdateIntegrationConnectionRequest request) {
        return ApiResponse.success(taskIntegrationService.updateConnection(id, request));
    }

    @DeleteMapping("/connections/{id}")
    public ApiResponse<Void> deleteConnection(@PathVariable String id) {
        taskIntegrationService.deleteConnection(id);
        return ApiResponse.success();
    }

    @PostMapping("/connection/test")
    public ApiResponse<IntegrationConnectionTestResultDto> testConnection() {
        return ApiResponse.success(taskIntegrationService.testConnection());
    }

    @GetMapping("/projects")
    public ApiResponse<List<ExternalProjectDto>> projects() {
        return ApiResponse.success(taskIntegrationService.projects());
    }

    @GetMapping("/projects/{projectId}/tasks")
    public ApiResponse<List<ExternalTaskDto>> tasks(
        @PathVariable String projectId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String priority,
        @RequestParam(required = false) String assigneeId,
        @RequestParam(required = false) String page,
        @RequestParam(required = false) String limit,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortOrder
    ) {
        return ApiResponse.success(taskIntegrationService.tasks(projectId, status, priority, assigneeId, page, limit, sortBy, sortOrder));
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ApiResponse<ExternalTaskDto> createTask(@PathVariable String projectId, @Valid @RequestBody CreateExternalTaskRequest request) {
        return ApiResponse.success(taskIntegrationService.createTask(projectId, request));
    }

    @PatchMapping("/tasks/{taskId}")
    public ApiResponse<ExternalTaskDto> updateTask(@PathVariable String taskId, @Valid @RequestBody UpdateExternalTaskRequest request) {
        return ApiResponse.success(taskIntegrationService.updateTask(taskId, request));
    }

    @PostMapping("/tasks/{taskId}/move")
    public ApiResponse<ExternalTaskDto> moveTask(@PathVariable String taskId, @Valid @RequestBody MoveExternalTaskRequest request) {
        return ApiResponse.success(taskIntegrationService.moveTask(taskId, request));
    }

    @GetMapping("/tasks/{taskId}/relations")
    public ApiResponse<List<TaskRelationDto>> relations(@PathVariable String taskId) {
        return ApiResponse.success(taskIntegrationService.listRelations(taskId));
    }

    @PostMapping("/tasks/{taskId}/relations")
    public ApiResponse<TaskRelationDto> createRelation(@PathVariable String taskId, @Valid @RequestBody CreateTaskRelationRequest request) {
        return ApiResponse.success(taskIntegrationService.createRelation(taskId, request));
    }

    @DeleteMapping("/tasks/{taskId}/relations/{relationId}")
    public ApiResponse<Void> deleteRelation(@PathVariable String taskId, @PathVariable String relationId) {
        taskIntegrationService.deleteRelation(relationId);
        return ApiResponse.success();
    }
}
