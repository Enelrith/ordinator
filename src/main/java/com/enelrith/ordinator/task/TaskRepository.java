package com.enelrith.ordinator.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    @Query("select t from Task t where t.taskOwner.project.id = ?1" +
            " order by" +
            " case" +
            " when t.status = ONGOING then 0" +
            " when t.status = ON_HOLD then 1" +
            " when t.status = COMPLETED then 2" +
            " when t.status = CANCELLED then 3" +
            " end," +
            " case" +
            " when t.importance = CRITICAL then 0" +
            " when t.importance = HIGH then 1" +
            " when t.importance = MEDIUM then 2" +
            " when t.importance = LOW then 3" +
            " end," +
            " t.updatedAt desc," +
            " t.id")
    List<Task> findAllByTaskOwner_Project_Id(UUID projectId);

    Optional<Task> findByIdAndTaskOwner_Project_Id(UUID taskId, UUID projectId);
    Optional<Task> findByIdAndTaskMembers_User_Email(UUID taskId, String userEmail);
    @Query("select t from Task t join t.taskOwner as to join to.user as tou join to.project as p join p.user as pu" +
            " where t.id = ?1 and (tou.email = ?2 or pu.email = ?2)")
    Optional<Task> findByIdAndTaskOwnerOrProjectAdmin(UUID id, String userEmail);

    boolean existsByNameAndTaskOwner_Project_Id(String name, UUID projectId);
    boolean existsByIdAndTaskOwner_User_Email(UUID id, String userEmail);
    boolean existsByIdAndTaskMembers_Id(UUID id, UUID projectMemberId);
    boolean existsByIdAndTaskMembers_User_Email(UUID id, String userEmail);
}