package com.engine.activity;

import java.util.Map;

public interface ActivityHandler {

    String getActivityName();

    Map<String, Object> execute(Map<String, Object> inputPayload) throws Exception;
    
}
