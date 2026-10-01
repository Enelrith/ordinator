package com.enelrith.ordinator.project;

import com.enelrith.ordinator.project.dto.ProjectInfoDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    @Query("""
    select new com.enelrith.ordinator.project.dto.ProjectInfoDto(
        p.id, p.name, p.status, count(distinct t.id))
    from Project p
    left join p.projectMembers pm
    left join pm.tasks t on t.status = ONGOING
    where exists (
              select 1
              from ProjectMember membership
              where membership.project = p
                and membership.user.email = ?1
        )
    group by p.id, p.name, p.status, p.updatedAt
    order by case
        when p.status = ONGOING then 0
        when p.status = COMPLETED then 1
        end,
        count(distinct t.id) desc,
        p.updatedAt desc
    """)
    List<ProjectInfoDto> findAllByUser_EmailWithOngoingTaskCount(String email);

    boolean existsByNameAndUser_Id(String name, UUID userId);
}