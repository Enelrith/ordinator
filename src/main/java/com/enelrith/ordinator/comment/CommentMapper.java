package com.enelrith.ordinator.comment;

import com.enelrith.ordinator.comment.dto.CommentDto;
import com.enelrith.ordinator.comment.dto.CreateCommentRequest;
import com.enelrith.ordinator.project.ProjectMember;
import com.enelrith.ordinator.project.ProjectMemberMapper;
import com.enelrith.ordinator.task.Task;

public class CommentMapper {
    private CommentMapper() {}

    public static Comment toEntity(CreateCommentRequest request, String attachmentName, String attachmentObjectKey,
                                   Task task, ProjectMember taskMember) {
        return new Comment(request.content(), attachmentName, attachmentObjectKey, task, taskMember);
    }

    public static CommentDto toCommentDto(Comment comment) {
        var taskMemberDto = comment.getTaskMember() == null ? null : ProjectMemberMapper.toProjectMemberDto(comment.getTaskMember());

        return new CommentDto(comment.getId(), comment.getContent(), comment.getCreatedAt(),
                comment.getAttachmentName(), taskMemberDto
        );
    }
}
