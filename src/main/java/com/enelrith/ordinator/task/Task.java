package com.enelrith.ordinator.task;

import com.enelrith.ordinator.common.BaseEntity;
import com.enelrith.ordinator.project.ProjectMember;
import jakarta.persistence.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {

    protected Task() {}

    public Task(String name, String description, TaskStatus status, TaskImportance importance, ProjectMember taskOwner) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.importance = importance;
        this.taskOwner = taskOwner;
    }

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskImportance importance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_member_id", nullable = false)
    private ProjectMember taskOwner;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "task_members",
            joinColumns = @JoinColumn(name = "task_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "project_member_id", nullable = false)
    )
    private Set<ProjectMember> taskMembers = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskImportance getImportance() {
        return importance;
    }

    public void setImportance(TaskImportance importance) {
        this.importance = importance;
    }

    public ProjectMember getTaskOwner() {
        return taskOwner;
    }

    public void setTaskOwner(ProjectMember projectMember) {
        this.taskOwner = projectMember;
    }

    public Set<ProjectMember> getTaskMembers() {
        return Collections.unmodifiableSet(taskMembers);
    }

    public void addProjectMember(ProjectMember projectMember) {
        taskMembers.add(projectMember);
    }
}
