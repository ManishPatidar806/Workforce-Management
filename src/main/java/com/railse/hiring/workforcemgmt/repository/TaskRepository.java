package com.railse.hiring.workforcemgmt.repository;


import com.railse.hiring.workforcemgmt.model.TaskActivity;
import com.railse.hiring.workforcemgmt.model.TaskComment;
import com.railse.hiring.workforcemgmt.model.TaskManagement;



import java.util.List;
import java.util.Optional;


public interface TaskRepository {
    Optional<TaskManagement> findById(Long id);
    TaskManagement save(TaskManagement task);
    List<TaskManagement> findAll();
    List<TaskManagement> findByReferenceIdAndReferenceType(Long referenceId, com.railse.hiring.workforcemgmt.common.model.enums.ReferenceType referenceType);
    List<TaskManagement> findByAssigneeIdIn(List<Long> assigneeIds);

    void addComment(TaskComment comment);

    List<TaskComment> getAllTaskComment();
    void addActivity(TaskActivity activity);

    List<TaskActivity> getAllTaskActivity();
}
