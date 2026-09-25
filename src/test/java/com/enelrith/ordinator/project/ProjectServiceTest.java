package com.enelrith.ordinator.project;

import com.enelrith.ordinator.common.exception.AlreadyExistsException;
import com.enelrith.ordinator.common.exception.NotAllowedException;
import com.enelrith.ordinator.common.exception.NotFoundException;
import com.enelrith.ordinator.project.dto.AddProjectMemberRequest;
import com.enelrith.ordinator.project.dto.CreateProjectRequest;
import com.enelrith.ordinator.user.User;
import com.enelrith.ordinator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProject_returnsProjectDto() {
        var user = buildTestUser();
        var request = new CreateProjectRequest("testName", "testDescription");

        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(projectRepository.existsByNameAndUser_Id(request.name(), user.getId())).thenReturn(false);

        var projectDto = projectService.createProject(request, user.getEmail());

        assertEquals(request.name(), projectDto.name());
        assertEquals(request.description(), projectDto.description());
        assertEquals(ProjectStatus.ONGOING, projectDto.status());

        verify(projectRepository).save(any(Project.class));
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    void createProject_withExistingProjectNameForUser_throwsAlreadyExistsException() {
        var user = buildTestUser();
        var request = new CreateProjectRequest("testName", "testDescription");

        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(projectRepository.existsByNameAndUser_Id(request.name(), user.getId())).thenReturn(true);

        var userEmail = user.getEmail();
        assertThrows(AlreadyExistsException.class, () -> projectService.createProject(request, userEmail));

        verify(projectRepository, never()).save(any(Project.class));
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @ParameterizedTest
    @CsvSource({
            "ADMIN, MANAGER",
            "ADMIN, MEMBER",
            "MANAGER, MEMBER"
    })
    void addProjectMember_returnsProjectMemberDto(ProjectMemberRole userRole, ProjectMemberRole inviteeRole) {
        var user = buildTestUser();
        var invitee = new User("invitee@email.com", "hashedPassword", "invitee", "invitee");
        var project = buildTestProject(user);
        var userMember = buildTestProjectMember(user, project, userRole);
        var request = new AddProjectMemberRequest(inviteeRole);
        var userId = UUID.randomUUID();
        var inviteeId = UUID.randomUUID();
        var projectId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(invitee, "id", inviteeId);
        ReflectionTestUtils.setField(project, "id", projectId);

        when(projectMemberRepository.existsByUser_IdAndProject_Id(any(), any())).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(projectMemberRepository.findByUser_IdAndProject_Id(userId, projectId)).thenReturn(Optional.of(userMember));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(inviteeId)).thenReturn(Optional.of(invitee));

        var projectMemberDto = projectService.addProjectMember(request, user.getEmail(), inviteeId, projectId);

        assertEquals(inviteeRole, projectMemberDto.role());
        assertEquals(invitee.getEmail(), projectMemberDto.user().email());

        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @ParameterizedTest
    @CsvSource({
            "ADMIN, ADMIN",
            "MANAGER, ADMIN",
            "MEMBER, ADMIN",
            "MANAGER, MANAGER",
            "MEMBER, MANAGER",
            "MEMBER, MEMBER"
    })
    void addProjectMember_withInvalidRole_throwsNotAllowedException(ProjectMemberRole userRole, ProjectMemberRole inviteeRole) {
        var user = buildTestUser();
        var project = buildTestProject(user);
        var invitee = buildTestInvitee();
        var userMember = buildTestProjectMember(user, project, userRole);
        var request = new AddProjectMemberRequest(inviteeRole);
        var userId = UUID.randomUUID();
        var inviteeId = UUID.randomUUID();
        var projectId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(any())).thenReturn(Optional.of(invitee));
        when(projectMemberRepository.existsByUser_IdAndProject_Id(any(), any())).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(projectMemberRepository.findByUser_IdAndProject_Id(userId, projectId)).thenReturn(Optional.of(userMember));

        var userEmail = user.getEmail();
        assertThrows(NotAllowedException.class, () -> projectService.addProjectMember(request, userEmail, inviteeId, projectId));

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void addProjectMember_withMissingProject_throwsNotFoundException() {
        var inviteeId = UUID.randomUUID();
        var projectId = UUID.randomUUID();
        var request = new AddProjectMemberRequest(ProjectMemberRole.MEMBER);

        when(projectMemberRepository.existsByUser_IdAndProject_Id(any(), any())).thenReturn(false);
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> projectService.addProjectMember(request, "test@email.com", inviteeId, projectId), "Project not found");

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void addProjectMember_withMissingInvitee_throwsNotFoundException() {
        var user = buildTestUser();
        var project = buildTestProject(user);
        var request = new AddProjectMemberRequest(ProjectMemberRole.MEMBER);
        var userId = UUID.randomUUID();
        var projectId = UUID.randomUUID();
        var inviteeId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(project, "id", projectId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(inviteeId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> projectService.addProjectMember(request, "test@email.com", inviteeId, projectId), "Invitee not found");

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void addProjectMember_withInviteeAsExistingMember_throwsAlreadyExistsException() {
        var request = new AddProjectMemberRequest(ProjectMemberRole.MEMBER);
        var inviteeId = UUID.randomUUID();
        var projectId = UUID.randomUUID();

        when(projectMemberRepository.existsByUser_IdAndProject_Id(any(), any())).thenReturn(true);

        assertThrows(AlreadyExistsException.class, () -> projectService.addProjectMember(request, "test@email.com", inviteeId, projectId));

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void addProjectMember_withUserNotMember_throwsNotFoundException() {
        var user = buildTestUser();
        var project = buildTestProject(user);
        var invitee = buildTestInvitee();
        var request = new AddProjectMemberRequest(ProjectMemberRole.MEMBER);
        var userId = UUID.randomUUID();
        var inviteeId = UUID.randomUUID();
        var projectId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(inviteeId)).thenReturn(Optional.of(invitee));
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByUser_IdAndProject_Id(any(), any())).thenReturn(false);

        var userEmail = user.getEmail();
        assertThrows(NotFoundException.class, () -> projectService.addProjectMember(request, userEmail, inviteeId, projectId), "Project member not found");

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void getProject_returnsProjectDto() {
        var user = buildTestUser();
        var project = buildTestProject(user);
        var projectMember = buildTestProjectMember(user, project, ProjectMemberRole.ADMIN);
        var projectId = UUID.randomUUID();
        ReflectionTestUtils.setField(project, "id", projectId);

        when(projectMemberRepository.existsByUser_EmailAndProject_Id(user.getEmail(), project.getId())).thenReturn(true);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectMemberRepository.findAllByProject_Id(project.getId())).thenReturn(List.of(projectMember));

        var projectDto = projectService.getProject(user.getEmail(), project.getId());

        assertEquals(project.getId(), projectDto.id());
    }

    @Test
    void getProject_withUserNotBeingMember_throwsNotFoundException() {
        var projectId = UUID.randomUUID();

        when(projectMemberRepository.existsByUser_EmailAndProject_Id(any(String.class), any(UUID.class))).thenReturn(false);

        assertThrows(NotFoundException.class, () -> projectService.getProject("test@email.com", projectId));
    }

    private User buildTestUser() {
        return new User("test@email.com", "hashedPassword", "test", "test");
    }

    private User buildTestInvitee() {
        return new User("invitee@email.com", "hashedPassword", "invitee", "invitee");
    }

    private Project buildTestProject(User user) {
        return new Project("testName", "testDescription", ProjectStatus.ONGOING, user);
    }

    private ProjectMember buildTestProjectMember(User user, Project project, ProjectMemberRole userRole) {
        return new ProjectMember(userRole, project, user);
    }
}
