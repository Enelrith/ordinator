package com.enelrith.ordinator.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    @Query("select pm from ProjectMember pm join fetch pm.user as u where pm.project.id = ?1" +
            " order by pm.user.firstName, pm.user.lastName, pm.id")
    List<ProjectMember> findAllByProject_Id(UUID projectId);

    Optional<ProjectMember> findByUser_IdAndProject_Id(UUID userId, UUID projectId);

    boolean existsByUser_IdAndProject_Id(UUID userId, UUID projectId);
    boolean existsByUser_EmailAndProject_Id(String userEmail, UUID projectId);
}
