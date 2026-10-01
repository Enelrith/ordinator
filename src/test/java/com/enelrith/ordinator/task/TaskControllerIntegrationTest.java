package com.enelrith.ordinator.task;

import com.enelrith.ordinator.TestcontainersConfiguration;
import com.enelrith.ordinator.project.*;
import com.enelrith.ordinator.task.dto.CreateTaskRequest;
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

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "test@email.com")
class TaskControllerIntegrationTest {
    private static final String TASKS_URI = "/api/tasks";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    User savedUser;
    Project savedProject;
    ProjectMember savedProjectMember;

    @BeforeEach
    void setup() {
        var user = new User("test@email.com", "hashedPassword", "test", "test");
        savedUser = userRepository.save(user);

        var project = new Project("test", "test", ProjectStatus.ONGOING, user);
        savedProject = projectRepository.save(project);

        var projectMember = new ProjectMember(ProjectMemberRole.ADMIN, project, user);
        savedProjectMember = projectMemberRepository.save(projectMember);
    }

    @Test
    void createTask_returnsCreated() throws Exception {
        var request = buildTestTaskRequest();

        mockMvc.perform(post(TASKS_URI + "/projects/" + savedProject.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.name").value(request.name()))
                .andExpect(jsonPath("$.description").value(request.description()))
                .andExpect(jsonPath("$.status").value("ONGOING"))
                .andExpect(jsonPath("$.importance").value("CRITICAL"))
                .andExpect(jsonPath("$.taskOwner.id").value(savedProjectMember.getId().toString()))
                .andExpect(jsonPath("$.taskMembers", hasSize(1)))
                .andExpect(jsonPath("$.taskMembers[0].id").value(savedProjectMember.getId().toString()));
    }

    @Test
    @WithAnonymousUser
    void createTask_withUnauthenticatedUser_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(TASKS_URI + "/projects/" + savedProject.getId())
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTask_withMissingCsrfToken_returnsForbidden() throws Exception {
        mockMvc.perform(post(TASKS_URI + "/projects/" + savedProject.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTask_withInvalidRequest_returnsBadRequest() throws Exception {
        var request = new CreateTaskRequest("  ", "test", TaskImportance.CRITICAL);

        mockMvc.perform(post(TASKS_URI + "/projects/" + savedProject.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_withMissingProject_returnsNotFound() throws Exception {
        var request = buildTestTaskRequest();

        mockMvc.perform(post(TASKS_URI + "/projects/" + UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "another.user@email.com")
    void createTask_withUserNotProjectMember_returnsNotFound() throws Exception {
        var request = buildTestTaskRequest();

        mockMvc.perform(post(TASKS_URI + "/projects/" + savedProject.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTask_withInvalidRole_returnsForbidden() throws Exception {
        var request = buildTestTaskRequest();
        savedProjectMember.setRole(ProjectMemberRole.MEMBER);

        mockMvc.perform(post(TASKS_URI + "/projects/" + savedProject.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_Task_withExistingTaskNameForProject_returnsConflict() throws Exception {
        var request = buildTestTaskRequest();

        var task = TaskMapper.toEntity(request, savedProjectMember);
        task.addProjectMember(savedProjectMember);
        taskRepository.save(task);

        mockMvc.perform(post(TASKS_URI + "/projects/" + savedProject.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithAnonymousUser
    void getAllProjectTaskInfo_withUnauthenticatedUser_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(buildGetAllProjectTaskInfoUri()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllProjectTaskInfo_withMissingProject_returnsNotFound() throws Exception {
        mockMvc.perform(get(TASKS_URI + "/projects/" + UUID.randomUUID() + "/info"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "another.user@email.com")
    void getAllProjectTaskInfo_withUserNotProjectMember_returnsNotFound() throws Exception {
        mockMvc.perform(get(buildGetAllProjectTaskInfoUri()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTask_withMissingTask_returnsNotFound() throws Exception {
        mockMvc.perform(get(TASKS_URI + "/" + UUID.randomUUID() + "/projects/" + savedProject.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "another.user@email.com")
    void getTask_withUserNotProjectMember_returnsNotFound() throws Exception {
        var task = new Task("test", "test", TaskStatus.ONGOING, TaskImportance.CRITICAL, savedProjectMember);
        taskRepository.save(task);

        assertNotNull(task.getId());

        mockMvc.perform(get(TASKS_URI + "/" + task.getId() + "/projects/" + savedProject.getId()))
                .andExpect(status().isNotFound());
    }

    @Nested
    class AddTask {
        Task savedTask;

        User savedUser2;

        ProjectMember savedProjectMemberToAdd;

        @BeforeEach
        void setup() {
            var task = new Task("test", "test", TaskStatus.ONGOING, TaskImportance.CRITICAL, savedProjectMember);
            task.addProjectMember(savedProjectMember);
            taskRepository.save(task);
            savedTask = task;

            var user2 = new User("test2@emai.com", "hashedPassword", "test", "test");
            userRepository.save(user2);
            savedUser2 = user2;

            var projectMemberToAdd = new ProjectMember(ProjectMemberRole.MEMBER, savedProject, savedUser2);
            projectMemberRepository.save(projectMemberToAdd);
            savedProjectMemberToAdd = projectMemberToAdd;
        }

        @Test
        void addsNewTaskMember_returnsNoContent() throws Exception {
            mockMvc.perform(post(TASKS_URI + "/" + savedTask.getId() + "/project-members/" + savedProjectMemberToAdd.getId())
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithAnonymousUser
        void withUnauthenticatedUser_returnsUnauthorized() throws Exception {
            mockMvc.perform(post(TASKS_URI + "/" + savedTask.getId() + "/project-members/" + savedProjectMemberToAdd.getId())
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void withMissingCsrfToken_returnsForbidden() throws Exception {
            mockMvc.perform(post(TASKS_URI + "/" + savedTask.getId() + "/project-members/" + savedProjectMemberToAdd.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void withMissingTask_returnsNotFound() throws Exception {
            mockMvc.perform(post(TASKS_URI + "/" + UUID.randomUUID() + "/project-members/" + savedProjectMemberToAdd.getId())
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "test3@email.com")
        void withProjectMemberNotTaskOwner_returnsNotFound() throws Exception {
            var user3 = new User("test3@email.com", "hashedPassword", "test", "test");
            userRepository.save(user3);

            var projectMember3 = new ProjectMember(ProjectMemberRole.MEMBER, savedProject, user3);
            projectMemberRepository.save(projectMember3);

            mockMvc.perform(post(TASKS_URI + "/" + savedTask.getId() + "/project-members/" + savedProjectMemberToAdd.getId())
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void withMissingProjectMember_returnsNotFound() throws Exception {
            mockMvc.perform(post(TASKS_URI + "/" + savedTask.getId() + "/project-members/" + UUID.randomUUID())
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void withExistingTaskProjectMember_returnsConflict() throws Exception {
            savedTask.addProjectMember(savedProjectMemberToAdd);

            mockMvc.perform(post(TASKS_URI + "/" + savedTask.getId() + "/project-members/" + savedProjectMemberToAdd.getId())
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }
    }

    private CreateTaskRequest buildTestTaskRequest() {
        return new CreateTaskRequest("test", "test", TaskImportance.CRITICAL);
    }

    private String buildGetAllProjectTaskInfoUri() {
        return TASKS_URI + "/projects/" + savedProject.getId() + "/info";
    }
}
