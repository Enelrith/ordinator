package com.enelrith.ordinator.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    @EntityGraph(attributePaths = {"taskMember", "taskMember.user"})
    Page<Comment> findAllByTask_IdAndTask_TaskOwner_Project_Id(UUID taskId, UUID projectId, Pageable pageable);

    Optional<Comment> findByIdAndTask_TaskMembers_User_Email(UUID id, String userEmail);

    boolean existsByAttachmentObjectKeyAndTask_Id(String attachmentObjectKey, UUID taskId);
}