package com.engine.model.request;

import java.util.Map;
import java.util.Objects;

import org.springframework.util.StringUtils;

import com.engine.model.WorkflowDefinition;

import tools.jackson.databind.ObjectMapper;

public record WorkFlowStartRequest(
    WorkflowDefinition definition,
    Map<String, Object> initialInput,
    String payload
) {
    public String getEffectivePayload(){
        if(StringUtils.hasText(payload)) 
            return payload;

        if(Objects.nonNull(initialInput)){
            try{
                return new ObjectMapper().writeValueAsString(initialInput);
            }catch(Exception e){
                return "{}";
            }
        }

        return null;
    }
}
