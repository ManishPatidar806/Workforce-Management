package com.railse.hiring.workforcemgmt.model;

import lombok.Data;

@Data
public class TaskActivity {
    private Long id;
    private Long taskId;
    private String description; // e.g., "Priority changed to HIGH"
    private Long createdBy;
    private Long timestamp;
}