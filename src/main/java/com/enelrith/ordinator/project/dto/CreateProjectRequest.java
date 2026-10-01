package com.enelrith.ordinator.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @Size(message = "{project.name.size}", min = 3, max = 50)
        @NotBlank(message = "{project.name.notBlank}") String name,
        @Size(message = "{project.description.size}", max = 500) String description) {
        public CreateProjectRequest {
                name = name == null || name.isBlank() ? null : name.strip();
                description = description == null || description.isBlank() ? null : description.strip();
        }
}