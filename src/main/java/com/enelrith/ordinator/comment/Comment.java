package com.enelrith.ordinator.comment;

import com.enelrith.ordinator.project.ProjectMember;
import com.enelrith.ordinator.task.Task;
import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments")
@EntityListeners(AuditingEntityListener.class)
public class Comment {

    protected Comment() {}

    public Comment(String content, String attachmentName, String attachmentObjectKey, Task task, ProjectMember taskMember) {
        this.content = content;
        this.attachmentName = attachmentName;
        this.attachmentObjectKey = attachmentObjectKey;
        this.task = task;
        this.taskMember = taskMember;
    }

    @Id
    @Generated
    private UUID id;

    @Column(nullable = false, length = 300, updatable = false)
    private String content;

    @Column(updatable = false)
    private String attachmentName;

    @Column(updatable = false)
    private String attachmentObjectKey;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, updatable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_member_id", updatable = false)
    private ProjectMember taskMember;

    public UUID getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public String getAttachmentObjectKey() {
        return attachmentObjectKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Task getTask() {
        return task;
    }

    public ProjectMember getTaskMember() {
        return taskMember;
    }
}
