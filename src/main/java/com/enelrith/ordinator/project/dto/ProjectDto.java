package com.enelrith.ordinator.project.dto;

import com.enelrith.ordinator.project.ProjectStatus;

import java.util.List;
import java.util.UUID;

public record ProjectDto(UUID id, String name, String description, ProjectStatus status, List<ProjectMemberDto> projectMembers) {
}