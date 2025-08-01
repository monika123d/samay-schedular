package com.samay.scheduler.model;

import com.samay.scheduler.condition.ConditionType;

public class ConditionDetail {
    private ConditionType type;
    private String value; // For FILE_EXTENSION
    private String startTime; // For TIME_WINDOW
    private String endTime;
    private  String fileCount;

    // Getters and setters


    public ConditionType getType() {
        return type;
    }

    public void setType(ConditionType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getFileCount() { return fileCount; }

    public void setFileCount(String fileCount) { this.fileCount = fileCount; }
}
