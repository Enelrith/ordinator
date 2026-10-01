package com.enelrith.ordinator.task;

import com.enelrith.ordinator.common.exception.AlreadyExistsException;
import com.enelrith.ordinator.common.exception.NotAllowedException;
import com.enelrith.ordinator.common.exception.NotFoundException;
import com.enelrith.ordinator.project.*;
import com.enelrith.ordinator.task.dto.CreateTaskRequest;
import com.enelrith.ordinator.user.User;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private TaskService taskService;

    @ParameterizedTest
    @CsvSource({"ADMIN", "MANAGER"})
    void createTask_returnsTaskDto(ProjectMemberRole projectMemberRole) {
        var request = buildTestCreateTaskRequest();
        var user = buildTestUser();
        var project = buildTestProject(user);
        var taskOwner = buildTestTaskOwner(project, user);
        taskOwner.setRole(projectMemberRole);
        assignIds(user, project, taskOwner, null);

        when(projectMemberRepository.findByUser_EmailAndProject_Id(user.getEmail(), project.getId()))
                .thenReturn(Optional.of(taskOwner));

        var taskDto = taskService.createTask(request, project.getId(), user.getEmail());

        assertEquals(request.name(), taskDto.name());
        assertEquals(request.description(), taskDto.description());
        assertEquals(TaskStatus.ONGOING, taskDto.status());
        assertEquals(request.importance(), taskDto.importance());
        assertEquals(taskOwner.getId(), taskDto.taskOwner().id());
        assertEquals(1, taskDto.taskMembers().size());
        assertTrue(taskDto.taskMembers().stream().findFirst().isPresent());
        assertEquals(taskOwner.getId(), taskDto.taskMembers().stream().findFirst().get().id());

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_withUserNotBeingProjectMember_throwsNotFoundException() {
        var request = buildTestCreateTaskRequest();
        var projectId = UUID.randomUUID();

        when(projectMemberRepository.findByUser_EmailAndProject_Id(any(String.class), any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taskService.createTask(request, projectId, "test@email.com"));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_withInvalidRole_throwsNotAllowedException() {
        var request = buildTestCreateTaskRequest();
        var user = buildTestUser();
        var project = buildTestProject(user);
        var taskOwner = buildTestTaskOwner(project, user);
        taskOwner.setRole(ProjectMemberRole.MEMBER);

        when(projectMemberRepository.findByUser_EmailAndProject_Id(user.getEmail(), project.getId()))
                .thenReturn(Optional.of(taskOwner));

        var projectId = project.getId();
        var userEmail = user.getEmail();
        assertThrows(NotAllowedException.class, () -> taskService.createTask(request, projectId, userEmail));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_withExistingTaskNameForProject_throwsAlreadyExistsException() {
        var request = buildTestCreateTaskRequest();
        var user = buildTestUser();
        var project = buildTestProject(user);
        var taskOwner = buildTestTaskOwner(project, user);

        when(projectMemberRepository.findByUser_EmailAndProject_Id(user.getEmail(), project.getId()))
                .thenReturn(Optional.of(taskOwner));
        when(taskRepository.existsByNameAndTaskOwner_Project_Id(request.name(), project.getId()))
                .thenReturn(true);

        var projectId = project.getId();
        var userEmail = user.getEmail();
        assertThrows(AlreadyExistsException.class, () -> taskService.createTask(request, projectId, userEmail));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void getAllProjectTaskInfo_returnsTaskInfoDtoList() {
        var user = buildTestUser();
        var project = buildTestProject(user);
        var taskOwner = buildTestTaskOwner(project, user);
        var task = buildTestTask(taskOwner);
        assignIds(user, project, taskOwner, null);

        var user2 = new User("test2@email.com", "hashedPassword", "test", "test");
        var taskOwner2 = buildTestTaskOwner(project, user2);
        var task2 = new Task("test2", "test", TaskStatus.ONGOING, TaskImportance.MEDIUM, taskOwner2);
        assignIds(user2, null, taskOwner2, null);

        when(projectMemberRepository.existsByUser_EmailAndProject_Id(user.getEmail(), project.getId()))
                .thenReturn(true);
        when(taskRepository.findAllByTaskOwner_Project_Id(project.getId())).thenReturn(List.of(task, task2));

        var taskInfoDtoList = taskService.getAllProjectTaskInfo(project.getId(), user.getEmail());

        assertEquals(2, taskInfoDtoList.size());
        assertEquals(task.getName(), taskInfoDtoList.getFirst().name());
        assertEquals(task.getStatus(), taskInfoDtoList.getFirst().status());
        assertEquals(task.getImportance(), taskInfoDtoList.getFirst().importance());
        assertEquals(task2.getName(), taskInfoDtoList.getLast().name());
    }

    @Test
    void getAllProjectTaskInfo_withUserNotBeingProjectMember_throwsNotFoundException() {
        var projectId = UUID.randomUUID();

        assertThrows(NotFoundException.class, () -> taskService.getAllProjectTaskInfo(projectId, "test@email.com"));
    }

    @Test
    void getTask_returnsTaskDto() {
        var user = buildTestUser();
        var project = buildTestProject(user);
        var taskOwner = buildTestTaskOwner(project, user);
        var task = buildTestTask(taskOwner);
        assignIds(user, project, taskOwner, task);

        when(projectMemberRepository.existsByUser_EmailAndProject_Id(user.getEmail(), project.getId()))
                .thenReturn(true);
        when(taskRepository.findByIdAndTaskOwner_Project_Id(task.getId(), project.getId())).thenReturn(Optional.of(task));
        when(projectMemberRepository.findAllByTasks_Id(task.getId())).thenReturn(List.of(taskOwner));

        var taskDto = taskService.getTask(task.getId(), project.getId(), user.getEmail());

        assertEquals(task.getId(), taskDto.id());
        assertEquals(1, taskDto.taskMembers().size());
        assertTrue(taskDto.taskMembers().stream().findFirst().isPresent());
        assertEquals(taskOwner.getId(), taskDto.taskMembers().stream().findFirst().get().id());
    }

    @Test
    void getTask_withMissingTaskOrUserNotAProjectMember_throwsNotFoundException() {
        var taskId = UUID.randomUUID();
        var projectId = UUID.randomUUID();

        when(projectMemberRepository.existsByUser_EmailAndProject_Id("test@email.com", projectId))
                .thenReturn(false);

        assertThrows(NotFoundException.class, () -> taskService.getTask(taskId, projectId , "test@email.com"));
    }

    @Test
    void addTaskMember_addsNewTaskMember() {
        var user = buildTestUser();
        var project = buildTestProject(user);
        var taskOwner = buildTestTaskOwner(project, user);
        var task = buildTestTask(taskOwner);
        task.addProjectMember(taskOwner);
        assignIds(user, project, taskOwner, task);

        var userToAdd = new User("invitee@email.com", "hashedPassword", "invitee", "invitee");
        var projectMemberToAdd = new ProjectMember(ProjectMemberRole.MEMBER, project, userToAdd);
        ReflectionTestUtils.setField(projectMemberToAdd, "id", UUID.randomUUID());


        when(taskRepository.existsByIdAndTaskOwner_User_Email(task.getId(), user.getEmail()))
                .thenReturn(true);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByIdAndProject_Id(projectMemberToAdd.getId(), project.getId()))
                .thenReturn(Optional.of(projectMemberToAdd));
        when(taskRepository.existsByIdAndTaskMembers_Id(task.getId(), projectMemberToAdd.getId()))
                .thenReturn(false);

        assertEquals(1, task.getTaskMembers().size());
        assertTrue(task.getTaskMembers().stream().findFirst().isPresent());
        assertEquals(taskOwner.getId(), task.getTaskMembers().stream().findFirst().get().getId());

        taskService.addTaskMember(task.getId(), projectMemberToAdd.getId(), user.getEmail());

        assertEquals(2, task.getTaskMembers().size());
        assertTrue(task.getTaskMembers().contains(taskOwner));
        assertTrue(task.getTaskMembers().contains(projectMemberToAdd));
    }

    @Test
    void addTaskMember_withMissingTaskOrUserNotTheTaskOwner_throwsNotFoundException() {
        var taskId = UUID.randomUUID();
        var projectMemberId = UUID.randomUUID();

        when(taskRepository.existsByIdAndTaskOwner_User_Email(taskId, "test@email.com"))
                .thenReturn(false);

        assertThrows(NotFoundException.class, () ->
                taskService.addTaskMember(taskId, projectMemberId, "test@email.com"),
                "Task not found or the user is not the task owner");
    }

    @Test
    void addTaskMember_withMissingProjectMember_throwsNotFoundException() {
        var user = buildTestUser();
        var project = buildTestProject(user);
        var taskOwner = buildTestTaskOwner(project, user);
        var task = buildTestTask(taskOwner);
        assignIds(user, project, taskOwner, task);
        var projectMemberId = UUID.randomUUID();
        var userEmail = "test@email.com";

        when(taskRepository.existsByIdAndTaskOwner_User_Email(task.getId(), userEmail))
                .thenReturn(true);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByIdAndProject_Id(projectMemberId, project.getId()))
                .thenReturn(Optional.empty());

        var taskId = task.getId();
        assertThrows(NotFoundException.class, () ->
                taskService.addTaskMember(taskId, projectMemberId, userEmail),
                "Project member not found");
    }

    @Test
    void addTaskMember_withExistingTaskProjectMember_throwsAlreadyExistsException() {
        var user = buildTestUser();
        var project = buildTestProject(user);
        var taskOwner = buildTestTaskOwner(project, user);
        var task = buildTestTask(taskOwner);
        assignIds(user, project, taskOwner, task);


        var userToAdd = new User("invitee@email.com", "hashedPassword", "invitee", "invitee");
        var projectMemberToAdd = new ProjectMember(ProjectMemberRole.MEMBER, project, userToAdd);
        ReflectionTestUtils.setField(projectMemberToAdd, "id", UUID.randomUUID());

        var userEmail = "test@email.com";
        var taskId = task.getId();
        var projectMemberToAddId = projectMemberToAdd.getId();

        when(taskRepository.existsByIdAndTaskOwner_User_Email(task.getId(), userEmail))
                .thenReturn(true);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByIdAndProject_Id(projectMemberToAdd.getId(), project.getId()))
                .thenReturn(Optional.of(projectMemberToAdd));
        when(taskRepository.existsByIdAndTaskMembers_Id(task.getId(), projectMemberToAdd.getId()))
                .thenReturn(true);

        assertThrows(AlreadyExistsException.class, () ->
                        taskService.addTaskMember(taskId, projectMemberToAddId, userEmail),
                "This member is already assigned to this task");
    }

    private CreateTaskRequest buildTestCreateTaskRequest() {
        return new CreateTaskRequest("test", "test", TaskImportance.CRITICAL);
    }

    private User buildTestUser() {
        return new User("test@email.com", "hashedPassword", "test", "test");
    }

    private Project buildTestProject(User user) {
        return new Project("testName", "testDescription", ProjectStatus.ONGOING, user);
    }

    private ProjectMember buildTestTaskOwner(Project project, User user) {
        return new ProjectMember(ProjectMemberRole.MANAGER, project, user);
    }

    private Task buildTestTask(ProjectMember taskOwner) {
        return new Task("test", "test", TaskStatus.ONGOING, TaskImportance.CRITICAL, taskOwner);
    }

    private void assignIds(User user, Project project, ProjectMember projectMember, Task task) {
        if (user != null) {
            var userId = UUID.randomUUID();
            ReflectionTestUtils.setField(user, "id", userId);
        }
        if (project != null) {
            var projectId = UUID.randomUUID();
            ReflectionTestUtils.setField(project, "id", projectId);
        }
        if (projectMember != null) {
            var projectMemberId = UUID.randomUUID();
            ReflectionTestUtils.setField(projectMember, "id", projectMemberId);
        }
        if (task != null) {
            var taskId = UUID.randomUUID();
            ReflectionTestUtils.setField(task, "id", taskId);
        }
    }
}
