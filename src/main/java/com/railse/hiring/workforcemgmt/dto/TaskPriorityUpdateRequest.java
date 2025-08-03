package com.railse.hiring.workforcemgmt.dto;
import com.railse.hiring.workforcemgmt.model.enums.Priority;
import lombok.Data;

@Data
public class TaskPriorityUpdateRequest {
    private Long taskId;
    private Priority priority;
}
