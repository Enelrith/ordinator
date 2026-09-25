package com.enelrith.ordinator.project;

import com.enelrith.ordinator.common.exception.AlreadyExistsException;
import com.enelrith.ordinator.common.exception.NotAllowedException;
import com.enelrith.ordinator.common.exception.NotFoundException;
import com.enelrith.ordinator.project.dto.AddProjectMemberRequest;
import com.enelrith.ordinator.project.dto.CreateProjectRequest;
import com.enelrith.ordinator.project.dto.ProjectDto;
import com.enelrith.ordinator.project.dto.ProjectMemberDto;
import com.enelrith.ordinator.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProjectService {
    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository, ProjectMemberRepository projectMemberRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional
    public ProjectDto createProject(CreateProjectRequest request, String userEmail) {
        var user = userRepository.findByEmailIgnoreCase(userEmail).orElseThrow(() -> new NotFoundException("User not found"));
        if (projectRepository.existsByNameAndUser_Id(request.name(), user.getId()))
            throw new AlreadyExistsException("A project with this name already exists");

        var project = ProjectMapper.toEntity(request, user);
        projectRepository.save(project);

        var projectMember = new ProjectMember(ProjectMemberRole.ADMIN, project, user);
        projectMemberRepository.save(projectMember);

        log.info("Created project {}", project.getId());

        return ProjectMapper.toProjectDto(project, List.of(projectMember));
    }

    @Transactional
    public ProjectMemberDto addProjectMember(AddProjectMemberRequest request, String userEmail, UUID inviteeId, UUID projectId) {
        if (projectMemberRepository.existsByUser_IdAndProject_Id(inviteeId, projectId)) throw new AlreadyExistsException("This user is already a member");
        var project = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
        var invitee = userRepository.findById(inviteeId).orElseThrow(() -> new NotFoundException("Invitee not found"));
        var user = userRepository.findByEmailIgnoreCase(userEmail).orElseThrow(() -> new NotFoundException("User not found"));
        var projectMember = projectMemberRepository.findByUser_IdAndProject_Id(user.getId(), projectId)
                .orElseThrow(() -> new NotFoundException("Project member not found"));

        if (
                (projectMember.getRole() == ProjectMemberRole.MANAGER && request.role() != ProjectMemberRole.MEMBER)
                || request.role() == ProjectMemberRole.ADMIN || projectMember.getRole() == ProjectMemberRole.MEMBER
        ) {
            throw new NotAllowedException();
        }

        var newProjectMember = ProjectMemberMapper.toEntity(request, project, invitee);
        projectMemberRepository.save(newProjectMember);

        log.info("Added user {} to project {} with role {}", user.getId(), project.getId(), request.role());

        return ProjectMemberMapper.toProjectMemberDto(newProjectMember);
    }

    public ProjectDto getProject(String userEmail, UUID projectId) {
        if (!projectMemberRepository.existsByUser_EmailAndProject_Id(userEmail, projectId))
            throw new NotFoundException("Project not found");
        var project = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
        var projectMembers = projectMemberRepository.findAllByProject_Id(project.getId());

        return ProjectMapper.toProjectDto(project, projectMembers);
    }
}
