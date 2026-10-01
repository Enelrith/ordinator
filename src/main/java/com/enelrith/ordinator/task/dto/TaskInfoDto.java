package com.enelrith.ordinator.task.dto;

import com.enelrith.ordinator.task.TaskImportance;
import com.enelrith.ordinator.task.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskInfoDto(UUID id, Instant updatedAt, String name, TaskStatus status, TaskImportance importance) {
}