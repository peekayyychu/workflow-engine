package com.engine.registry;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.engine.model.WorkflowDefinition;
import com.engine.model.WorkflowStep;

@Configuration
public class WorkflowConfiguration {
    
    @Bean
    public WorkflowDefinition initWorkflowConfig(){
        return new WorkflowDefinition(
            List.of(
                new WorkflowStep("CREATE_USER_ACCOUNT", Map.of("role", "user"), 3),
                new WorkflowStep("SEND_WELCOME_EMAIL", Map.of("template", "WELCOME"), 3),
                new WorkflowStep("PROVISION_STORAGE", Map.of("quotaGb", 5), 3)
            )
        );
    };
}

