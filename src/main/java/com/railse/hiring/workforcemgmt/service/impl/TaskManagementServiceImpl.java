package com.railse.hiring.workforcemgmt.service.impl;


import com.railse.hiring.workforcemgmt.common.exception.ResourceNotFoundException;
import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.mapper.ITaskManagementMapper;

import com.railse.hiring.workforcemgmt.model.TaskActivity;
import com.railse.hiring.workforcemgmt.model.TaskComment;
import com.railse.hiring.workforcemgmt.model.TaskManagement;
import com.railse.hiring.workforcemgmt.model.enums.Priority;
import com.railse.hiring.workforcemgmt.model.enums.Task;


import com.railse.hiring.workforcemgmt.model.enums.TaskStatus;
import com.railse.hiring.workforcemgmt.repository.TaskRepository;
import com.railse.hiring.workforcemgmt.service.TaskManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/*
 * Note:
 * This project does not include a User entity or authentication mechanism.
 * To simulate user-based actions like activity logging or commenting,
 * a random or default userId is generated for demonstration purposes.
 */


@Service
public class TaskManagementServiceImpl implements TaskManagementService {

    private final TaskRepository taskRepository;
    private final ITaskManagementMapper taskMapper;


    public TaskManagementServiceImpl(TaskRepository taskRepository, ITaskManagementMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    public TaskManagementDto findTaskById(Long id) {
        TaskManagement task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        TaskManagementDto dto = taskMapper.modelToDto(task);
        List<TaskComment> comments = taskRepository.getAllTaskComment().stream()
                .filter(c -> c.getTaskId().equals(task.getId()))
                .sorted(Comparator.comparing(TaskComment::getTimestamp))
                .toList();
        List<TaskActivity> activities = taskRepository.getAllTaskActivity().stream()
                .filter(a -> a.getTaskId().equals(task.getId()))
                .sorted(Comparator.comparing(TaskActivity::getTimestamp))
                .toList();
        dto.setComments(comments);
        dto.setActivityHistory(activities);
        return dto;
    }



    @Override
    public List<TaskManagementDto> createTasks(TaskCreateRequest createRequest) {
        List<TaskManagement> createdTasks = new ArrayList<>();
        Random random = new Random();
        int userId = random.nextInt();
        for (TaskCreateRequest.RequestItem item : createRequest.getRequests()) {
            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(item.getReferenceId());
            newTask.setReferenceType(item.getReferenceType());
            newTask.setTask(item.getTask());
            newTask.setAssigneeId(item.getAssigneeId());
            newTask.setPriority(item.getPriority());
            newTask.setCreatedTime(System.currentTimeMillis());
            newTask.setTaskDeadlineTime(item.getTaskDeadlineTime());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("New task created.");
            createdTasks.add(taskRepository.save(newTask));
            logActivity(newTask.getId(), "User " + userId + " created this task", (long)userId );

        }
        return taskMapper.modelListToDtoList(createdTasks);
    }


    @Override
    public List<TaskManagementDto> updateTasks(UpdateTaskRequest updateRequest) {
        List<TaskManagement> updatedTasks = new ArrayList<>();
        Random random = new Random();
        int userId = random.nextInt();
        for (UpdateTaskRequest.RequestItem item : updateRequest.getRequests()) {
            TaskManagement task = taskRepository.findById(item.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + item.getTaskId()));


            if (item.getTaskStatus() != null) {
                task.setStatus(item.getTaskStatus());
            }
            if (item.getDescription() != null) {
                task.setDescription(item.getDescription());
            }
            updatedTasks.add(taskRepository.save(task));
            logActivity(task.getId(), "User " + userId + " updated task status to " + item.getTaskStatus(), (long)userId);

        }
        return taskMapper.modelListToDtoList(updatedTasks);
    }


    @Override
    public String assignByReference(AssignByReferenceRequest request) {
        List<Task> applicableTasks = Task.getTasksByReferenceType(request.getReferenceType());
        List<TaskManagement> existingTasks = taskRepository.findByReferenceIdAndReferenceType(request.getReferenceId(), request.getReferenceType());


        for (Task taskType : applicableTasks) {
            List<TaskManagement> tasksOfType = existingTasks.stream()
                    .filter(t -> t.getTask() == taskType && t.getStatus() != TaskStatus.COMPLETED)
                    .toList();

            // BUG #1 is here. It should assign one and cancel the rest.
            // Instead, it reassigns ALL of them.
            if (!tasksOfType.isEmpty()) {
                for (TaskManagement taskToUpdate : tasksOfType) {
                    taskToUpdate.setStatus(TaskStatus.CANCELLED);
                    taskRepository.save(taskToUpdate);
                }
            } else {
                // Create a new task if none exist
                TaskManagement newTask = new TaskManagement();
                newTask.setReferenceId(request.getReferenceId());
                newTask.setReferenceType(request.getReferenceType());
                newTask.setTask(taskType);
                newTask.setAssigneeId(request.getAssigneeId());
                newTask.setStatus(TaskStatus.ASSIGNED);
                taskRepository.save(newTask);
            }
        }
        return "Tasks assigned successfully for reference " + request.getReferenceId();
    }

    /*
     * Implementing Feature 1: Smart Daily Task View
     * To support this feature, I added a new field called createdTime to the TaskManagement model.
     */
    @Override
    public List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request) {
        List<TaskManagement> tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());
        Long start = request.getStartDate();
        Long end = request.getEndDate();

        if (start != null && end != null) {
            List<TaskManagement> smartTasks = tasks.stream()
                    .filter(task -> task.getStatus() == TaskStatus.ASSIGNED || task.getStatus() == TaskStatus.STARTED)
                    .filter(task ->
                            (task.getCreatedTime() != null && task.getCreatedTime() >= start && task.getCreatedTime() <= end)
                                    || (task.getCreatedTime() != null && task.getCreatedTime() < start)
                    )
                    .collect(Collectors.toList());

            return taskMapper.modelListToDtoList(smartTasks);
        }

        // Existing Bug Fix: filter out CANCELLED tasks
        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.CANCELLED)
                .collect(Collectors.toList());

        return taskMapper.modelListToDtoList(filteredTasks);
    }

    @Override
    public String updatePriority(TaskPriorityUpdateRequest priorityUpdateRequest) {
        Optional<TaskManagement> task= taskRepository.findById(priorityUpdateRequest.getTaskId());
        Random random = new Random();
        int userId = random.nextInt();
        if(task.isEmpty()){
            return "Task is Not Found";
        }
        task.get().setPriority(priorityUpdateRequest.getPriority());
        logActivity(task.get().getId(), "User " + userId + " changed priority to " + priorityUpdateRequest.getPriority(), (long)userId);

        return "Priority Updated SuccessFully";
    }

    @Override
    public List<TaskManagementDto> getTasksByPriority(Priority priority) {
        List<TaskManagement> taskManagements = taskRepository.findAll().stream().filter(
                task-> task.getPriority()==priority
        ).toList();
        return taskMapper.modelListToDtoList(taskManagements);
    }
    static Long commentId = 0L;
    public void addComment(Long taskId, String commentText, Long userId) {
        TaskComment comment = new TaskComment();
        comment.setId(++commentId);
        comment.setTaskId(taskId);
        comment.setCommentText(commentText);
        comment.setCreatedBy(userId);
        comment.setTimestamp(System.currentTimeMillis());
        logActivity(taskId, "User " + userId + " added a comment", userId);
        taskRepository.addComment(comment);
    }

    static Long logId = 0L;
    public void logActivity(Long taskId, String description, Long userId) {
        TaskActivity activity = new TaskActivity();
        activity.setId(++logId);
        activity.setTaskId(taskId);
        activity.setDescription(description);
        activity.setCreatedBy(userId);
        activity.setTimestamp(System.currentTimeMillis());
        taskRepository.addActivity(activity);
    }

}


