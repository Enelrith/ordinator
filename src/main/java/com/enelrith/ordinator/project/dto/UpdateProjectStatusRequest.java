package com.enelrith.ordinator.project.dto;

import com.enelrith.ordinator.project.ProjectStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectStatusRequest(@NotNull ProjectStatus status) {
}