package com.enelrith.ordinator.project;

import com.enelrith.ordinator.common.BaseEntity;
import com.enelrith.ordinator.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "project_members")
public class ProjectMember extends BaseEntity {

    protected ProjectMember() {}

    public ProjectMember(ProjectMemberRole role, Project project, User user) {
        this.role = role;
        this.project = project;
        this.user = user;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectMemberRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public ProjectMemberRole getRole() {
        return role;
    }

    public void setRole(ProjectMemberRole role) {
        this.role = role;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
