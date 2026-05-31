package com.veritas.backend.integrations.jira.service;

import com.veritas.backend.integrations.jira.entity.JiraConfig;
import com.veritas.backend.integrations.jira.repository.JiraConfigRepository;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@Slf4j
public class DynamicJiraScheduler {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final JiraConfigRepository repository;
    private final JiraSyncService syncService;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public DynamicJiraScheduler(JiraConfigRepository repository,
                                @Lazy JiraSyncService syncService) {
        this.repository = repository;
        this.syncService = syncService;
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(5);
        this.taskScheduler.setThreadNamePrefix("JiraSync-");
        this.taskScheduler.initialize();
    }

    @PostConstruct
    public void init() {
        log.info("Initializing dynamic Jira sync schedules...");
        List<JiraConfig> configs = repository.findAllByIsActiveTrue();
        for (JiraConfig config : configs) {
            scheduleConfig(config);
        }

        taskScheduler.scheduleWithFixedDelay(
            () -> {
                try {
                    syncService.processQueue();
                } catch (RuntimeException e) {
                    log.error("Error processing Jira sync queue", e);
                }
            },
            Instant.now().plus(java.time.Duration.ofSeconds(15)),
            Duration.ofSeconds(15)
        );
    }

    public void scheduleConfig(JiraConfig config) {
        cancelConfig(config.getId());
        if (config.isActive() && config.getSyncIntervalMinutes() != null && config.getSyncIntervalMinutes() > 0) {
            Runnable task = () -> {
                log.info("Running scheduled sync for config: {}", config.getName());
                syncService.runManualSync(config.getId());
            };

            ScheduledFuture<?> future = taskScheduler.scheduleWithFixedDelay(
                task,
                Instant.now().plus(Duration.ofSeconds(10)),
                Duration.ofMinutes(config.getSyncIntervalMinutes())
            );
            scheduledTasks.put(config.getId(), future);
            log.info("Scheduled sync for config ID {} every {} minutes", config.getId(),
                config.getSyncIntervalMinutes());
        }
    }

    public void cancelConfig(Long id) {
        ScheduledFuture<?> future = scheduledTasks.remove(id);
        if (future != null) {
            future.cancel(false);
            log.info("Cancelled sync for config ID {}", id);
        }
    }
}
