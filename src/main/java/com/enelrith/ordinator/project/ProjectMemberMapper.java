package com.enelrith.ordinator.project;

import com.enelrith.ordinator.project.dto.AddProjectMemberRequest;
import com.enelrith.ordinator.project.dto.ProjectMemberDto;
import com.enelrith.ordinator.user.User;
import com.enelrith.ordinator.user.UserMapper;

public class ProjectMemberMapper {
    private ProjectMemberMapper() {}

    public static ProjectMember toEntity(AddProjectMemberRequest request, Project project, User user) {
        return new ProjectMember(request.role(), project, user);
    }

    public static ProjectMemberDto toProjectMemberDto(ProjectMember projectMember) {
        return new ProjectMemberDto(projectMember.getId(), projectMember.getRole(), UserMapper.toUserDto(projectMember.getUser()));
    }
}
