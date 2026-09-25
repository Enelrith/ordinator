package com.enelrith.ordinator.project;

import com.enelrith.ordinator.common.BaseEntity;
import com.enelrith.ordinator.user.User;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    protected Project() {}

    public Project(String name, String description, ProjectStatus status, User user) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.user = user;
    }

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private List<ProjectMember> projectMembers = new ArrayList<>();

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

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<ProjectMember> getProjectMembers() {
        return Collections.unmodifiableList(projectMembers);
    }
}
