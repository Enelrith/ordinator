package com.enelrith.ordinator.comment.dto;

import com.enelrith.ordinator.project.dto.ProjectMemberDto;

import java.time.Instant;
import java.util.UUID;

public record CommentDto(UUID id, String content, Instant createdAt, String attachmentName, ProjectMemberDto author) {
}