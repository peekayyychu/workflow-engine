package com.engine.activity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component 
public class ActivityTimerRegistry {
    
    private final Set<String> activeTimers = ConcurrentHashMap.newKeySet();

    private String buildKey(String workflowId, String timerName){
        return workflowId + ":" + timerName;
    }

    public void register(String workflowId, String timerName){
        activeTimers.add(buildKey(workflowId, timerName));
    }

    public void unregister(String workflowId, String timerName){
        activeTimers.remove(buildKey(workflowId, timerName));
    }

    public boolean isActive(String workflowId, String timerName){
        return activeTimers.contains(buildKey(workflowId, timerName));
    }
}
