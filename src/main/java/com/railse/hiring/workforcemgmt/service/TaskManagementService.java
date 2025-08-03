package com.railse.hiring.workforcemgmt.service;


import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.model.enums.Priority;


import java.util.List;


public interface TaskManagementService {
    List<TaskManagementDto> createTasks(TaskCreateRequest request);
    List<TaskManagementDto> updateTasks(UpdateTaskRequest request);
    String assignByReference(AssignByReferenceRequest request);
    List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request);
    TaskManagementDto findTaskById(Long id);
    String updatePriority(TaskPriorityUpdateRequest priorityUpdateRequest);

    List<TaskManagementDto> getTasksByPriority(Priority priority);
    void addComment(Long taskId, String commentText, Long userId);

    void logActivity(Long taskId, String description, Long userId);

}
