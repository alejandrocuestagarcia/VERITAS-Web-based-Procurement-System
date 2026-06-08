package com.veritas.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class TestAsyncConfig {

    @Bean
    public Executor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
