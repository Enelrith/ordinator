package com.enelrith.ordinator.task.dto;

import com.enelrith.ordinator.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(@NotNull(message = "{task.status.notNull}") TaskStatus status) {
}