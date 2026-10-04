package com.enelrith.ordinator.task.dto;

import com.enelrith.ordinator.project.ProjectStatus;
import com.enelrith.ordinator.project.dto.ProjectMemberDto;
import com.enelrith.ordinator.task.TaskImportance;
import com.enelrith.ordinator.task.TaskStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TaskDto(UUID id, Instant updatedAt, String name, String description, TaskStatus status, ProjectStatus projectStatus,
                      TaskImportance importance, ProjectMemberDto taskOwner, Set<ProjectMemberDto> taskMembers, UUID projectAdminId) {
}