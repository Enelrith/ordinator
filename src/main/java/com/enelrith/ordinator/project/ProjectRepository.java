package com.enelrith.ordinator.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    boolean existsByNameAndUser_Id(String name, UUID userId);
}