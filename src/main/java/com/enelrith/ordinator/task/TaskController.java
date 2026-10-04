package com.enelrith.ordinator.task;

import com.enelrith.ordinator.task.dto.CreateTaskRequest;
import com.enelrith.ordinator.task.dto.TaskDto;
import com.enelrith.ordinator.task.dto.TaskInfoDto;
import com.enelrith.ordinator.task.dto.UpdateTaskStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Creates a new task in the database and adds the current user as a task member")
    @ApiResponse(responseCode = "201", description = "Task created and user added as member")
    @ApiResponse(responseCode = "400", description = "Invalid content in the request body")
    @ApiResponse(responseCode = "401", description = "User is not authenticated")
    @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token or user does not have a valid role")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @ApiResponse(responseCode = "409", description = "A task with the given name already exists for the project")
    @PostMapping("/projects/{projectId}")
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody CreateTaskRequest request,
                                              @PathVariable UUID projectId,
                                              Authentication authentication) {
        var taskDto = taskService.createTask(request, projectId, authentication.getName());
        var uri = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .pathSegment(taskDto.id().toString())
                .build()
                .toUri();
        return ResponseEntity.created(uri).body(taskDto);
    }

    @Operation(summary = "Retrieves minimal information about all the tasks of a project")
    @ApiResponse(responseCode = "200", description = "Task information retrieved successfully")
    @ApiResponse(responseCode = "401", description = "User is not authenticated")
    @ApiResponse(responseCode = "404", description = "Project does not exist or the user is not a member of it")
    @GetMapping("/projects/{projectId}/info")
    public ResponseEntity<List<TaskInfoDto>> getAllProjectTaskInfo(@PathVariable UUID projectId,
                                                                   Authentication authentication) {
        var taskInfoDtoList = taskService.getAllProjectTaskInfo(projectId, authentication.getName());

        return ResponseEntity.ok(taskInfoDtoList);
    }

    @Operation(summary = "Retrieves detailed information about a specific task")
    @ApiResponse(responseCode = "200", description = "Task retrieved successfully")
    @ApiResponse(responseCode = "401", description = "User is not authenticated")
    @ApiResponse(responseCode = "404", description = "Task does not exists or the user is not a member of it")
    @GetMapping("/{taskId}/projects/{projectId}")
    public ResponseEntity<TaskDto> getTask(@PathVariable UUID taskId,
                                           @PathVariable UUID projectId,
                                           Authentication authentication) {
        var taskDto = taskService.getTask(taskId, projectId, authentication.getName());

        return ResponseEntity.ok(taskDto);
    }

    @Operation(summary = "Adds an existing project member as a task member for a task belonging to the same project")
    @ApiResponse(responseCode = "204", description = "Task member added successfully")
    @ApiResponse(responseCode = "401", description = "User is not authenticated")
    @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token")
    @ApiResponse(
            responseCode = "404",
            description = "Task does not exist, the current user is not the task owner," +
            " or the invitee is not a member of the project"
    )
    @ApiResponse(responseCode = "409", description = "The invitee is already a member of this task")
    @PostMapping("/{taskId}/project-members/{projectMemberId}")
    public ResponseEntity<Void> addTaskMember(@PathVariable UUID taskId,
                                              @PathVariable UUID projectMemberId,
                                              Authentication authentication) {
        taskService.addTaskMember(taskId, projectMemberId, authentication.getName());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<Void> updateTaskStatus(@Valid @RequestBody UpdateTaskStatusRequest request,
                                                 @PathVariable UUID taskId,
                                                 Authentication authentication) {
        taskService.updateTaskStatus(request, taskId, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
