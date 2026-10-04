package com.enelrith.ordinator.comment;

import com.enelrith.ordinator.comment.dto.AttachmentUploadAttempt;
import com.enelrith.ordinator.comment.dto.CommentAttachmentInfo;
import com.enelrith.ordinator.comment.dto.CommentDto;
import com.enelrith.ordinator.comment.dto.CreateCommentRequest;
import com.enelrith.ordinator.common.exception.NotAllowedException;
import com.enelrith.ordinator.common.exception.NotFoundException;
import com.enelrith.ordinator.common.exception.NotValidException;
import com.enelrith.ordinator.project.ProjectMemberRepository;
import com.enelrith.ordinator.project.ProjectStatus;
import com.enelrith.ordinator.s3.S3ClientService;
import com.enelrith.ordinator.task.TaskRepository;
import com.enelrith.ordinator.task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CommentService {
    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final S3ClientService s3ClientService;
    private final ApplicationEventPublisher eventPublisher;

    public CommentService(CommentRepository commentRepository,
                          TaskRepository taskRepository, ProjectMemberRepository projectMemberRepository,
                          S3ClientService s3ClientService, ApplicationEventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.s3ClientService = s3ClientService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CommentDto createComment(CreateCommentRequest commentRequest, MultipartFile attachmentFile,
                                    UUID taskId, String userEmail) {
        var task = taskRepository.findByIdAndTaskMembers_User_Email(taskId, userEmail)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        var projectMember = projectMemberRepository.findByUser_EmailAndProject_Id(userEmail, task.getTaskOwner().getProject().getId())
                .orElseThrow(() -> new NotFoundException("Project member not found"));
        if (task.getStatus() != TaskStatus.ONGOING || projectMember.getProject().getStatus() != ProjectStatus.ONGOING) {
            throw new NotAllowedException("This task cannot be modified");
        }

        Comment comment;
        if (attachmentFile != null && !attachmentFile.isEmpty()) {
            var fileName = attachmentFile.getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                throw new NotValidException("Attachment name cannot be empty");
            } else if (fileName.length() > 255) {
                throw new NotValidException("Attachment name cannot exceed 255 characters");
            }

            var attachmentObjectKey = UUID.randomUUID() + "-" + UUID.randomUUID();
            comment = CommentMapper.toEntity(commentRequest, fileName,
                    attachmentObjectKey, task, projectMember
            );

            eventPublisher.publishEvent(new AttachmentUploadAttempt(attachmentObjectKey));

            s3ClientService.putObject(comment.getAttachmentObjectKey(), attachmentFile);
        } else {
            comment = CommentMapper.toEntity(commentRequest, null, null, task, projectMember);
        }

        commentRepository.save(comment);

        var commentDto = CommentMapper.toCommentDto(comment);

        log.info("Created comment {}", commentDto.id());

        return commentDto;
    }

    public Page<CommentDto> getAllTaskComments(UUID projectId, UUID taskId, String userEmail, Pageable pageable) {
        if (!projectMemberRepository.existsByUser_EmailAndProject_Id(userEmail, projectId))
            throw new NotFoundException("Project member not found");

        var comments = commentRepository.findAllByTask_IdAndTask_TaskOwner_Project_Id(taskId, projectId, pageable);

        return comments.map(CommentMapper::toCommentDto);
    }

    public CommentAttachmentInfo getCommentAttachmentInfo(UUID commentId, String userEmail) {
        var comment = commentRepository.findByIdAndTask_TaskMembers_User_Email(commentId, userEmail)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        if (comment.getAttachmentName() == null || comment.getAttachmentObjectKey() == null) {
            throw new NotFoundException("Attachment not found");
        }

        return new CommentAttachmentInfo(comment.getAttachmentName(), comment.getAttachmentObjectKey());
    }
}
