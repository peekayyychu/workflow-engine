package com.engine.service;

import java.util.Map;

public sealed interface WorkFlowDecision {
    record ScheduleActivity(String activityType, Map<String, Object> input) implements WorkFlowDecision {}
    record CompleteWorkflow(Map<String, Object> output) implements WorkFlowDecision {}
    record FailWorkflow(String reason) implements WorkFlowDecision {}
    record Wait() implements WorkFlowDecision {}
}