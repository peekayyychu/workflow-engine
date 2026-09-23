package com.engine.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration  
@EnableAsync 
public class TaskExecutorConfiguration {

    public TaskExecutor applicationTaskExecutor(){
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
            .name("workflow-vt-", 0)
            .factory();

        ExecutorService executorService = Executors.newThreadPerTaskExecutor(virtualThreadFactory);

        return new TaskExecutorAdapter(executorService);
    }
}
