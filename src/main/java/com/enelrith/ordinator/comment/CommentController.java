package com.enelrith.ordinator.comment;

import com.enelrith.ordinator.comment.dto.CommentDto;
import com.enelrith.ordinator.comment.dto.CreateCommentRequest;
import com.enelrith.ordinator.s3.S3ClientService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;
    private final S3ClientService s3ClientService;

    public CommentController(CommentService commentService, S3ClientService s3ClientService) {
        this.commentService = commentService;
        this.s3ClientService = s3ClientService;
    }

    @PostMapping(value = "/tasks/{taskId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommentDto> createComment(@Valid @RequestPart(name = "commentRequest") CreateCommentRequest commentRequest,
                                        @RequestPart(name = "attachmentFile", required = false) MultipartFile attachmentFile,
                                        @PathVariable UUID taskId, Authentication authentication) {
        var commentDto = commentService.createComment(commentRequest, attachmentFile, taskId, authentication.getName());
        var uri = UriComponentsBuilder
                .fromUriString("/api/comments")
                .pathSegment(commentDto.id().toString())
                .build()
                .toUri();
        return ResponseEntity.created(uri).body(commentDto);
    }

    @GetMapping("/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<Page<CommentDto>> getAllTaskComments(@PathVariable UUID projectId, @PathVariable UUID taskId,
                                                               Authentication authentication,
                                                               @PageableDefault(
                                                                       sort = "createdAt",
                                                                       direction = Sort.Direction.DESC
                                                               ) Pageable pageable) {
        var commentDtoPage = commentService.getAllTaskComments(projectId, taskId, authentication.getName(), pageable);

        return ResponseEntity.ok(commentDtoPage);
    }

    @GetMapping("/{commentId}/attachment")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable UUID commentId, Authentication authentication) {
        var commentAttachmentInfo = commentService.getCommentAttachmentInfo(commentId, authentication.getName());
        var attachmentFile = s3ClientService.getObjectAsByteArray(commentAttachmentInfo.attachmentObjectKey());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(commentAttachmentInfo.attachmentName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(attachmentFile);
    }
}
