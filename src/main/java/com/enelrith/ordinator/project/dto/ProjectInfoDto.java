package com.enelrith.ordinator.project.dto;

import com.enelrith.ordinator.project.ProjectStatus;

import java.util.UUID;

public record ProjectInfoDto(UUID id, String name, ProjectStatus status, long ongoingTaskCount) {
}