package com.enelrith.ordinator.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @Size(message = "{comment.content.size}", max = 300)
        @NotBlank(message = "{comment.content.notBlank}") String content) {
  public CreateCommentRequest {
    content = content == null || content.isBlank() ? null : content.strip();
  }
}