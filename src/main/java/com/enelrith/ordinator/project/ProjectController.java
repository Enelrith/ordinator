package com.enelrith.ordinator.project;

import com.enelrith.ordinator.project.dto.*;
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
@RequestMapping("/api/projects")
@Tag(name = "Projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "Creates a new project in the database and adds the current user as an admin member")
    @ApiResponse(responseCode = "201", description = "Project created and user added as a member")
    @ApiResponse(responseCode = "400", description = "Invalid content in the request body")
    @ApiResponse(responseCode = "401", description = "User is not authenticated")
    @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token")
    @ApiResponse(responseCode = "409", description = "The user making the request already has a project with the same name")
    @PostMapping
    public ResponseEntity<ProjectDto> createProject(@Valid @RequestBody CreateProjectRequest request,
                                                    Authentication authentication) {
        var projectDto = projectService.createProject(request, authentication.getName());
        var uri = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .pathSegment(projectDto.id().toString())
                .build()
                .toUri();
        return ResponseEntity.created(uri).body(projectDto);
    }

    @Operation(
            summary = "Adds a new member to an existing project",
            description = "Uses another user's email to retrieve them and add them as a member for a project." +
                    "If the user has the ADMIN role, they can add members with the MANAGER and MEMBER roles." +
                    "If the user has the MANAGER role, they can only add members with the MEMBER role." +
                    "If the user has the MEMBER role, they cannot add new members at all"
    )
    @ApiResponse(responseCode = "201", description = "Member added to project")
    @ApiResponse(responseCode = "400", description = "Invalid content in the request body")
    @ApiResponse(responseCode = "401", description = "User is not authenticated")
    @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token or the user's role is invalid")
    @ApiResponse(responseCode = "404", description = "Missing project, invitee, or the current user is not a member of the project")
    @ApiResponse(responseCode = "409", description = "The invitee is already a member of the project")
    @PostMapping("/{projectId}/users/{inviteeEmail}")
    public ResponseEntity<ProjectMemberDto> addProjectMember(@Valid @RequestBody AddProjectMemberRequest request,
                                                             Authentication authentication,
                                                             @PathVariable String inviteeEmail,
                                                             @PathVariable UUID projectId) {
        var projectMemberDto = projectService.addProjectMember(request, authentication.getName(), inviteeEmail, projectId);
        var uri = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .pathSegment("project-members", projectMemberDto.id().toString())
                .build()
                .toUri();
        return ResponseEntity.created(uri).body(projectMemberDto);
    }

    @Operation(summary = "Fetches a project with all its members")
    @ApiResponse(responseCode = "200", description = "Project fetched successfully")
    @ApiResponse(responseCode = "401", description = "User is not authenticated")
    @ApiResponse(responseCode = "404", description = "Project does not exist or the user is not a member of it")
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDto> getProject(Authentication authentication, @PathVariable UUID projectId) {
        var projectDto = projectService.getProject(authentication.getName(), projectId);

        return ResponseEntity.ok(projectDto);
    }

    @Operation(summary = "Fetches minimal information about all the projects a user is a member of")
    @ApiResponse(responseCode = "200", description = "Project information fetched successfully")
    @ApiResponse(responseCode = "401", description = "User is not authenticated")
    @GetMapping("/info")
    public ResponseEntity<List<ProjectInfoDto>> getAllUserProjectInfo(Authentication authentication) {
        var projectInfoDtoList = projectService.getAllUserProjectInfo(authentication.getName());

        return ResponseEntity.ok(projectInfoDtoList);
    }

    @Operation(summary = "Fetches all projects members of a project the user is a member of")
    @ApiResponse(responseCode = "200", description = "Members fetched successfully")
    @ApiResponse(responseCode = "401", description = "User is not authenticated")
    @GetMapping("/{projectId}/project-members")
    public ResponseEntity<List<ProjectMemberDto>> getAllProjectMembers(@PathVariable UUID projectId,
                                                                       Authentication authentication) {
        var projectMemberDtoList = projectService.getAllProjectMembers(projectId, authentication.getName());

        return ResponseEntity.ok(projectMemberDtoList);
    }
}
