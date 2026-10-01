package com.enelrith.ordinator.task.dto;

import com.enelrith.ordinator.task.TaskImportance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @Size(message = "{task.name.size}", max = 100)
        @NotBlank(message = "{task.name.notBlank}") String name,
        @Size(message = "{task.description.size}", max = 500) String description,
        @NotNull(message = "{task.importance.notNull}") TaskImportance importance) {
        public CreateTaskRequest {
                name = name == null || name.isBlank() ? null : name.strip();
                description = description == null || description.isBlank() ? null : description.strip();
        }
}