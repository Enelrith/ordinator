package com.enelrith.ordinator.project;

import com.enelrith.ordinator.TestcontainersConfiguration;
import com.enelrith.ordinator.common.exception.NotFoundException;
import com.enelrith.ordinator.project.dto.AddProjectMemberRequest;
import com.enelrith.ordinator.project.dto.CreateProjectRequest;
import com.enelrith.ordinator.user.User;
import com.enelrith.ordinator.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "test@email.com")
class ProjectControllerIntegrationTest {
    private static final String PROJECTS_URI = "/api/projects";

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    User savedUser;
    @BeforeEach
    void setup() {
        var user = buildTestUser();
        userRepository.save(user);
        savedUser = user;
    }

    @Test
    void createProject_returnsCreated() throws Exception {
        var request = new CreateProjectRequest("testName", "testDescription");

        mockMvc.perform(post(PROJECTS_URI)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(request.name()))
                .andExpect(jsonPath("$.description").value(request.description()))
                .andExpect(jsonPath("$.status").value("ONGOING"))
                .andExpect(jsonPath("$.projectMembers", hasSize(1)))
                .andExpect(jsonPath("$.projectMembers[0].id").exists())
                .andExpect(jsonPath("$.projectMembers[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.projectMembers[0].user.id").value(savedUser.getId().toString()));
    }

    @Test
    void createProject_withInvalidRequest_returnsBadRequest() throws Exception {
        var request = new CreateProjectRequest(null, "test");

        mockMvc.perform(post(PROJECTS_URI)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void createProject_withUnauthenticatedUser_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(PROJECTS_URI)
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProject_withMissingCsrfToken_returnsForbidden() throws Exception {
        mockMvc.perform(post(PROJECTS_URI))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProject_withExistingProjectNameForUser_returnsConflict() throws Exception {
        var request = new CreateProjectRequest("testName", "testDescription");
        var user = userRepository.findById(savedUser.getId()).orElseThrow(() -> new NotFoundException("User not found"));
        var project = buildTestProject(user);
        projectRepository.save(project);

        mockMvc.perform(post(PROJECTS_URI)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Nested
    class AddProjectMembers {
        User savedInvitee;
        Project savedProject;
        ProjectMember savedProjectMember;
        @BeforeEach
        void setup() {
            var project = buildTestProject(savedUser);
            projectRepository.save(project);
            savedProject = project;

            var projectMember = buildTestProjectMember(savedUser, project, ProjectMemberRole.ADMIN);
            projectMemberRepository.save(projectMember);
            savedProjectMember = projectMember;

            var invitee = buildTestInvitee();
            userRepository.save(invitee);
            savedInvitee = invitee;
        }

        @Test
        void returnsCreated() throws Exception {
            var request = new AddProjectMemberRequest(ProjectMemberRole.MEMBER);

            mockMvc.perform(post(buildAddProjectMemberUri(savedProject.getId(), savedInvitee.getId()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.role").value("MEMBER"))
                    .andExpect(jsonPath("$.user.id").value(savedInvitee.getId().toString()))
                    .andExpect(jsonPath("$.user.email").value(savedInvitee.getEmail()))
                    .andExpect(jsonPath("$.user.firstName").value(savedInvitee.getFirstName()))
                    .andExpect(jsonPath("$.user.lastName").value(savedInvitee.getLastName()));
        }

        @Test
        void withInvalidRole_returnsForbidden() throws Exception {
            var request = new AddProjectMemberRequest(ProjectMemberRole.MEMBER);
            savedProjectMember.setRole(ProjectMemberRole.MEMBER);

            mockMvc.perform(post(buildAddProjectMemberUri(savedProject.getId(), savedInvitee.getId()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithAnonymousUser
        void withUnauthenticatedUser_returnsUnauthorized() throws Exception {
            mockMvc.perform(post(buildAddProjectMemberUri(savedProject.getId(), savedInvitee.getId()))
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void withMissingCsrfToken_returnsForbidden() throws Exception {
            mockMvc.perform(post(buildAddProjectMemberUri(savedProject.getId(), savedInvitee.getId())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void withInviteeAsExistingMember_returnsConflict() throws Exception {
            var inviteeMember = buildTestProjectMember(savedInvitee, savedProject, ProjectMemberRole.MEMBER);
            projectMemberRepository.save(inviteeMember);

            var request = new AddProjectMemberRequest(ProjectMemberRole.MEMBER);

            mockMvc.perform(post(buildAddProjectMemberUri(savedProject.getId(), savedInvitee.getId()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void withMissingProject_returnsNotFound() throws Exception {
            var request = new AddProjectMemberRequest(ProjectMemberRole.MEMBER);

            mockMvc.perform(post(buildAddProjectMemberUri(UUID.randomUUID(), savedInvitee.getId()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void withMissingInvitee_returnsNotFound() throws Exception {
            var request = new AddProjectMemberRequest(ProjectMemberRole.MEMBER);

            mockMvc.perform(post(buildAddProjectMemberUri(savedProject.getId(), UUID.randomUUID()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "another.user@email.com")
        void withUserNotBeingMember_returnsNotFound() throws Exception {
            var anotherUser = new User("another.user@email.com", "hashedPassword", "test", "test");
            userRepository.save(anotherUser);

            var request = new AddProjectMemberRequest(ProjectMemberRole.MEMBER);

            mockMvc.perform(post(buildAddProjectMemberUri(savedProject.getId(), savedInvitee.getId()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void getProject_returnsOk() throws Exception {
        var project = buildTestProject(savedUser);
        projectRepository.save(project);

        var user1 = new User("user.1@email.com", "hashedPassword", "Alfred", "Clancy");
        var user2 = new User("user.2@email.com", "hashedPassword", "Benjamin", "Johnson");
        var user3 = new User("user.3@email.com", "hashedPassword", "Benjamin", "Berkeley");
        userRepository.saveAll(List.of(user1, user2, user3));

        var savedUserMember = buildTestProjectMember(savedUser, project, ProjectMemberRole.ADMIN);
        var projectMember1 = buildTestProjectMember(user1, project, ProjectMemberRole.MEMBER);
        var projectMember2 = buildTestProjectMember(user2, project, ProjectMemberRole.MEMBER);
        var projectMember3 = buildTestProjectMember(user3, project, ProjectMemberRole.MEMBER);
        projectMemberRepository.saveAll(List.of(savedUserMember, projectMember1, projectMember2, projectMember3));

        mockMvc.perform(get(PROJECTS_URI + "/" + project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId().toString()))
                .andExpect(jsonPath("$.name").value(project.getName()))
                .andExpect(jsonPath("$.description").value(project.getDescription()))
                .andExpect(jsonPath("$.status").value("ONGOING"))
                .andExpect(jsonPath("$.projectMembers", hasSize(4)))
                .andExpect(jsonPath("$.projectMembers[0].id").value(projectMember1.getId().toString()))
                .andExpect(jsonPath("$.projectMembers[1].id").value(projectMember3.getId().toString()))
                .andExpect(jsonPath("$.projectMembers[2].id").value(projectMember2.getId().toString()))
                .andExpect(jsonPath("$.projectMembers[3].id").value(savedUserMember.getId().toString()));
    }

    @Test
    @WithAnonymousUser
    void getProject_withUnauthenticatedUser_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(PROJECTS_URI + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProject_withCurrentUserNotMember_returnsNotFound() throws Exception {
        var project = buildTestProject(savedUser);
        projectRepository.save(project);

        mockMvc.perform(get(PROJECTS_URI + "/" + project.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProject_withMissingProject_returnsNotFound() throws Exception {
        var project = buildTestProject(savedUser);
        projectRepository.save(project);

        var savedUserMember = buildTestProjectMember(savedUser, project, ProjectMemberRole.ADMIN);
        projectMemberRepository.save(savedUserMember);

        mockMvc.perform(get(PROJECTS_URI + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
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

    private String buildAddProjectMemberUri(UUID projectId, UUID inviteeId) {
        return PROJECTS_URI + "/" + projectId + "/users/" + inviteeId;
    }
}
