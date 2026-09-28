package com.engine.registry;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.engine.model.WorkflowDefinition;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service 
@RequiredArgsConstructor 
public class RedisWorkflowDefinitionRegistry implements WorkflowDefinitionRegistry{

    private static final String TEMPLATE_PREFIX = "wf:template:";
    private static final String INSTANCE_PREFIX = "wf:instance:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void registerTemplate(String definitionKey, WorkflowDefinition definition) {
        saveToRedis(TEMPLATE_PREFIX + definitionKey, definition);
    }

    @Override
    public WorkflowDefinition getTemplate(String definitionKey) {
        return getFromRedis(TEMPLATE_PREFIX + definitionKey);
    }

    @Override
    public void associateInstance(String workflowId, WorkflowDefinition defintion) {
        saveToRedis(INSTANCE_PREFIX + workflowId, defintion);
    }

    @Override
    public WorkflowDefinition getForInstance(String workflowId) {
        return getFromRedis(INSTANCE_PREFIX + workflowId);
    }

    private void saveToRedis(String key, WorkflowDefinition defintion){
        try{
            String json = objectMapper.writeValueAsString(defintion);
            redisTemplate.opsForValue().set(key, json);
        }catch(Exception e){
            throw new RuntimeException("Failed to serialize workflow definition to Redis", e);
        }
    }

    private WorkflowDefinition getFromRedis(String key){
        try {
            String json = redisTemplate.opsForValue().get(key);
            if(!StringUtils.hasText(json)){
                return null;
            }

            return objectMapper.readValue(json, WorkflowDefinition.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deseralize worfklow defintion from Redis", e);
        }
    }
    
}
