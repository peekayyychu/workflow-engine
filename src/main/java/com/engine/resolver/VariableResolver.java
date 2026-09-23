package com.engine.resolver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.StringBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.engine.exception.UnresolvedVariableException;
import com.engine.model.WorkFlowEvent;
import com.engine.model.WorkFlowState;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.engine.model.WorkFlowEvent.EventType;

@Component 
public class VariableResolver {
    private static final Pattern EXPRESSION_PATTERN = 
        Pattern.compile("\\$\\{([a-zA-Z0-9_-]+)\\.([^}]+)\\}");

    private static final Pattern PURE_EXPRESSION_PATTERN = 
        Pattern.compile("^\\$\\{([a-zA-Z0-9_-]+)\\.([^}]+)\\}$");

    @Autowired 
    private ObjectMapper objectMapper;

    public Map<String, Object> resolve(Map<String, Object> rawInput, WorkFlowState state, String workflowId, String currentStep){
        Map<String, Object> output = new LinkedHashMap<>();

        for(Map.Entry<String, Object> input : rawInput.entrySet()){
            Object resolvedValue = resolveValue(input.getValue(), state, workflowId, currentStep);

            output.put(input.getKey(), resolvedValue);
        }   

        return output;
    }

    private Object resolveValue(Object value, WorkFlowState state, String workflowId, String currentStep) {
        return switch (value) {
            case Map<?, ?> map -> resolve((Map<String, Object>) map, state, workflowId, currentStep);
            case List<?> list -> resolveList(list, state, workflowId, currentStep);
            case String string -> resolveString(string, state, workflowId, currentStep);
            default -> value;
        };
    }


    private Object resolveString(String input, WorkFlowState state, String workflowId, String currentStep){
        Matcher pureMatcher = PURE_EXPRESSION_PATTERN.matcher(input);

        if(pureMatcher.matches()){
            String stepName = pureMatcher.group(1);
            String path = pureMatcher.group(2);

            return extractFromHistory(stepName, path, state, workflowId, currentStep);
        }

        Matcher matcher = EXPRESSION_PATTERN.matcher(input);

        if(!matcher.find()){
            return input;
        }

        matcher.reset();
        StringBuilder sb = new StringBuilder();

        while(matcher.find()){
            String stepName = matcher.group(1);
            String path = matcher.group(2);

            Object resolvedValue = extractFromHistory(stepName, path, state, workflowId, currentStep);

            String replacement = String.valueOf(resolvedValue);

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private Object extractFromHistory(String stepName, String path, WorkFlowState state, String workflowId, String currentStep){
        WorkFlowEvent completedEvent = state.getEvents().stream()
            .filter(event -> event.getEventType().equals(EventType.ACTIVITY_COMPLETED))
            .filter(event -> stepName.equals(event.getActivityName()))
            .findFirst()
            .orElseThrow(()-> new UnresolvedVariableException(
                workflowId,
                currentStep,
                "${" + stepName + "." + path + "}",
                "Step '" + stepName + "' has not completed or does not exist in history"
            ));

        return navigatePath(completedEvent.getPayload(), path, workflowId, currentStep, stepName);
    }

    private List<?> resolveList(List<?> value, WorkFlowState state, String workflowId, String currentStep){
        List<Object> resolvedList = new ArrayList<>();

        for(var it: value){
            Object resolveValue = resolveValue(it, state, workflowId, currentStep);
            resolvedList.add(resolveValue);
        }

        return resolvedList;
    }

    @SuppressWarnings("null")
    private Object navigatePath(Object payload, String path, String workflowId, String currentStep, String stepName){
        if(Objects.isNull(payload)){
            throw new UnresolvedVariableException(
                workflowId,
                currentStep,
                "${" + stepName + "." + path + "}",
                "Payload for step '" + stepName + "' is null"
            );
        }

        Object targetPayload = payload;

        if(payload instanceof String jsonString){
            try {
                targetPayload = objectMapper.readTree(jsonString);
            } catch (Exception e) {
                throw new UnresolvedVariableException(
                    workflowId,
                    stepName, 
                    "${" + stepName + "." + path + "}",
                    "Failed to parse payload JSON for step '" + stepName + "': " + e.getMessage()
                );
            }
        }

        String [] segments = path.split("\\.");

        Object current = targetPayload;

        for(int i = 0; i<segments.length; i++){
            String segment = segments[i];

            if(Objects.isNull(current)){
                throw new UnresolvedVariableException(
                    workflowId,
                    currentStep,
                    "${" + stepName + "." + path + "}",
                    "Payload is null, payload:" + current
                );
            }

            switch (current) {
                case JsonNode node -> {
                    if(!node.has(segment)){
                        throw new UnresolvedVariableException(
                                workflowId,
                                currentStep,
                                "${" + stepName + "." + path + "}",
                                "Property segment '" + segment + "' does not exist in path '" + path + "'"
                        );
                    }
                    
                    JsonNode childNode = node.get(segment);
                    current = unwrapJsonNode(childNode);
                }
                case Map<?,?> map -> {
                    if(!map.containsKey(segment)){
                        throw new UnresolvedVariableException(
                                workflowId,
                                currentStep,
                                "${" + stepName + "." + path + "}",
                                "Property segment '" + segment + "' does not exist in path '" + path + "'"
                        );
                    }
                    current = map.get(segment);
                }
                default -> throw new UnresolvedVariableException(
                        workflowId,
                        currentStep,
                        "${" + stepName + "." + path + "}",
                        "Cannot navigate segment '" + segment + "' on target object of type " + (current != null ? current.getClass().getName() : "null")
                );
            }

            if(Objects.isNull(current) && i<segments.length - 1){
                throw new UnresolvedVariableException(
                    workflowId,
                    currentStep,
                    "${" + stepName + "." + path + "}",
                    "Intermediate property segment '" + segment + "' evaluated to null"
                );
            }
        }

        return current;
    }

    private Object unwrapJsonNode(JsonNode node){
        if(Objects.isNull(node) || node.isNull() || node.isMissingNode()) return null;
        

        return switch (node.getNodeType()){
            case STRING -> node.asString();
            case BOOLEAN -> node.asBoolean();
            case NUMBER -> {
                if(node.isInt()) yield node.asInt();
                if(node.isLong()) yield node.asLong();
                yield node.asDouble();
            }
            case ARRAY, OBJECT, BINARY, POJO -> node;
            default -> node.asString();
        };
    }
}
