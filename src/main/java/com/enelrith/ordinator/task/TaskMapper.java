package com.enelrith.ordinator.task;

import com.enelrith.ordinator.project.ProjectMember;
import com.enelrith.ordinator.project.ProjectMemberMapper;
import com.enelrith.ordinator.task.dto.CreateTaskRequest;
import com.enelrith.ordinator.task.dto.TaskDto;
import com.enelrith.ordinator.task.dto.TaskInfoDto;

import java.util.List;
import java.util.stream.Collectors;

public class TaskMapper {
    private TaskMapper() {}

    public static Task toEntity(CreateTaskRequest request, ProjectMember taskOwner) {
        return new Task(request.name(),request.description(), TaskStatus.ONGOING, request.importance(), taskOwner);
    }

    public static TaskDto toTaskDto(Task task, List<ProjectMember> taskMembers) {
        var taskMemberDtoSet = taskMembers.stream().map(ProjectMemberMapper::toProjectMemberDto).collect(Collectors.toSet());

        return new TaskDto(task.getId(), task.getUpdatedAt(), task.getName(), task.getDescription(), task.getStatus(),
                task.getImportance(), ProjectMemberMapper.toProjectMemberDto(task.getTaskOwner()), taskMemberDtoSet
        );
    }

    public static TaskInfoDto toTaskInfoDto(Task task) {
        return new TaskInfoDto(task.getId(), task.getUpdatedAt(), task.getName(), task.getStatus(), task.getImportance());
    }
}
