package com.enelrith.ordinator.project;

import com.enelrith.ordinator.project.dto.CreateProjectRequest;
import com.enelrith.ordinator.project.dto.ProjectDto;
import com.enelrith.ordinator.user.User;

import java.util.List;

public class ProjectMapper {
    private ProjectMapper() {}

    public static Project toEntity(CreateProjectRequest request, User user) {
        return new Project(request.name(), request.description(), ProjectStatus.ONGOING, user);
    }

    public static ProjectDto toProjectDto(Project project, List<ProjectMember> projectMembers) {
        var projectMemberDtos = projectMembers.stream().map(ProjectMemberMapper::toProjectMemberDto).toList();

        return new ProjectDto(project.getId(), project.getName(), project.getDescription(), project.getStatus(), projectMemberDtos);
    }
}
