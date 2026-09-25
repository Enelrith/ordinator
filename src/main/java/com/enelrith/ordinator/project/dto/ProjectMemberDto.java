package com.enelrith.ordinator.project.dto;

import com.enelrith.ordinator.project.ProjectMemberRole;
import com.enelrith.ordinator.user.dto.UserDto;

import java.util.UUID;

public record ProjectMemberDto(UUID id, ProjectMemberRole role, UserDto user) {
}