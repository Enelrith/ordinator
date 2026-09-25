package com.enelrith.ordinator.project.dto;

import com.enelrith.ordinator.project.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;

public record AddProjectMemberRequest(@NotNull(message = "{projectMember.role.notNull}") ProjectMemberRole role) {
}