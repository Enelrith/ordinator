package com.enelrith.ordinator;

import com.enelrith.ordinator.comment.dto.AttachmentUploadAttempt;
import com.enelrith.ordinator.s3.S3ClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CommentEventListener {
    private static final Logger log = LoggerFactory.getLogger(CommentEventListener.class);

    private final S3ClientService s3ClientService;

    public CommentEventListener(S3ClientService s3ClientService) {
        this.s3ClientService = s3ClientService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void cleanUpAttachment(AttachmentUploadAttempt event) {
        try {
            s3ClientService.deleteObject(event.attachmentObjectKey());
        } catch (RuntimeException _) {
            log.error("Failed to clean up attachment {}", event.attachmentObjectKey());
        }
    }
}
