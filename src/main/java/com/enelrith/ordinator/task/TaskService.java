package com.enelrith.ordinator.task;

import com.enelrith.ordinator.common.exception.AlreadyExistsException;
import com.enelrith.ordinator.common.exception.NotAllowedException;
import com.enelrith.ordinator.common.exception.NotFoundException;
import com.enelrith.ordinator.project.ProjectMemberRepository;
import com.enelrith.ordinator.project.ProjectMemberRole;
import com.enelrith.ordinator.project.ProjectStatus;
import com.enelrith.ordinator.task.dto.CreateTaskRequest;
import com.enelrith.ordinator.task.dto.TaskDto;
import com.enelrith.ordinator.task.dto.TaskInfoDto;
import com.enelrith.ordinator.task.dto.UpdateTaskStatusRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly=true)
public class TaskService {
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private static final String TASK_NOT_FOUND = "Task not found";
    
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public TaskService(TaskRepository taskRepository, ProjectMemberRepository projectMemberRepository) {
        this.taskRepository = taskRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional
    public TaskDto createTask(CreateTaskRequest request, UUID projectId, String userEmail) {
        var taskOwner = projectMemberRepository.findByUser_EmailAndProject_Id(userEmail, projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        if (taskOwner.getProject().getStatus() != ProjectStatus.ONGOING) {
            throw new NotAllowedException("This project cannot be modified");
        }
        if (taskOwner.getRole() == ProjectMemberRole.MEMBER) {
            throw new NotAllowedException("You are not allowed to perform this action");
        }
        if (taskRepository.existsByNameAndTaskOwner_Project_Id(request.name(), projectId)) {
            throw new AlreadyExistsException("A task with this name already exists for this project");
        }

        var task = TaskMapper.toEntity(request, taskOwner);
        task.addProjectMember(taskOwner);
        taskRepository.save(task);

        log.info("Created task {}", task.getId());

        return TaskMapper.toTaskDto(task, List.of(taskOwner));
    }

    public List<TaskInfoDto> getAllProjectTaskInfo(UUID projectId, String userEmail) {
        if (!projectMemberRepository.existsByUser_EmailAndProject_Id(userEmail, projectId)) {
            throw new NotFoundException("Project not found");
        }
        var tasks = taskRepository.findAllByTaskOwner_Project_Id(projectId);

        return tasks.stream().map(TaskMapper::toTaskInfoDto).toList();
    }

    public TaskDto getTask(UUID taskId, UUID projectId, String userEmail) {
        if (!projectMemberRepository.existsByUser_EmailAndProject_Id(userEmail, projectId)) {
            throw new NotFoundException("Project not found");
        }
        var task = taskRepository.findByIdAndTaskOwner_Project_Id(taskId, projectId)
                .orElseThrow(() -> new NotFoundException(TASK_NOT_FOUND));
        var taskMembers = projectMemberRepository.findAllByTasks_Id(task.getId());

        return TaskMapper.toTaskDto(task, taskMembers);
    }

    @Transactional
    public void addTaskMember(UUID taskId, UUID projectMemberId, String userEmail) {
        if (!taskRepository.existsByIdAndTaskOwner_User_Email(taskId, userEmail)) {
            throw new NotFoundException("Task not found or the user is not the task owner");
        }
        var task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException(TASK_NOT_FOUND));
        var projectMember = projectMemberRepository.findByIdAndProject_Id(projectMemberId, task.getTaskOwner().getProject().getId())
                .orElseThrow(() -> new NotFoundException("Project member not found"));
        if (task.getStatus() != TaskStatus.ONGOING || projectMember.getProject().getStatus() != ProjectStatus.ONGOING) {
            throw new NotAllowedException("This task cannot be modified");
        }
        if (taskRepository.existsByIdAndTaskMembers_Id(taskId, projectMemberId)) {
            throw new AlreadyExistsException("This member is already assigned to this task");
        }

        task.addProjectMember(projectMember);

        log.info("Added project member {} to task {}", projectMemberId, taskId);
    }

    @Transactional
    public void updateTaskStatus(UpdateTaskStatusRequest request, UUID taskId, String userEmail) {
        var task = taskRepository.findByIdAndTaskOwnerOrProjectAdmin(taskId, userEmail)
                .orElseThrow(() -> new NotFoundException(TASK_NOT_FOUND));
        if (task.getTaskOwner().getProject().getStatus() != ProjectStatus.ONGOING) {
            throw new NotAllowedException("This task cannot be modified");
        }

        task.setStatus(request.status());
    }
}
