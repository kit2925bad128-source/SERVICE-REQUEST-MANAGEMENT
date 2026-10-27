package com.servicerequest;

import java.time.LocalDateTime;

public class ServiceRequest {
    private int requestId;
    private int userId;
    private String title;
    private String description;
    private String category;
    private String priority;
    private Integer assignedTo;
    private String assignedEmployeeName;
    private String status;
    private String resolution;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public ServiceRequest() {
    }

    public ServiceRequest(int requestId, int userId, String title, String description,
                          String category, String priority, Integer assignedTo, String status,
                          String resolution, LocalDateTime createdAt, LocalDateTime resolvedAt) {
        this.requestId = requestId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.assignedTo = assignedTo;
        this.status = status;
        this.resolution = resolution;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Integer assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getAssignedEmployeeName() {
        return assignedEmployeeName;
    }

    public void setAssignedEmployeeName(String assignedEmployeeName) {
        this.assignedEmployeeName = assignedEmployeeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}