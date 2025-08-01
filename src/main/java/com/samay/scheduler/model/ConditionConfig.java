package com.samay.scheduler.model;

import java.util.List;

public class ConditionConfig {

    private String operator; // AND or OR
    private List<ConditionDetail> conditions;

    public List<ConditionDetail> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionDetail> conditions) {
        this.conditions = conditions;
    }

    // Getters and setters

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
