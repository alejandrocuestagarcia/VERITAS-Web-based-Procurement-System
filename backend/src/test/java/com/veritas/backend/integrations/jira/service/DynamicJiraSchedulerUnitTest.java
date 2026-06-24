package com.veritas.backend.integrations.jira.service;

import com.veritas.backend.integrations.jira.entity.JiraConfig;
import com.veritas.backend.integrations.jira.repository.JiraConfigRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicJiraSchedulerUnitTest {

    @Mock
    private JiraConfigRepository repository;

    @Mock
    private JiraSyncService syncService;

    private DynamicJiraScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DynamicJiraScheduler(repository, syncService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void init_WithEmptyConfigs_BypassesLoop() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> scheduler.init());

        verify(repository).findAll();
        Map<Long, ScheduledFuture<?>> scheduledTasks = 
            (Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(scheduler, "scheduledTasks");
        assertNotNull(scheduledTasks);
        assertTrue(scheduledTasks.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void init_WithActiveConfigs_SchedulesSync() {
        JiraConfig config = new JiraConfig();
        config.setId(10L);
        config.setName("Veritas Jira");
        config.setSyncIntervalMinutes(5);

        when(repository.findAll()).thenReturn(List.of(config));

        assertDoesNotThrow(() -> scheduler.init());

        verify(repository).findAll();
        Map<Long, ScheduledFuture<?>> scheduledTasks = 
            (Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(scheduler, "scheduledTasks");
        assertNotNull(scheduledTasks);
        assertEquals(1, scheduledTasks.size());
        assertTrue(scheduledTasks.containsKey(10L));

        // Clean up scheduler thread pool by cancelling task
        scheduler.cancelConfig(10L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void scheduleConfig_WithIntervalZeroOrNegative_DoesNotSchedule() {
        JiraConfig config = new JiraConfig();
        config.setId(20L);
        config.setName("Inactive Jira");
        config.setSyncIntervalMinutes(0);

        scheduler.scheduleConfig(config);

        Map<Long, ScheduledFuture<?>> scheduledTasks = 
            (Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(scheduler, "scheduledTasks");
        assertNotNull(scheduledTasks);
        assertFalse(scheduledTasks.containsKey(20L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void cancelConfig_WithNonExistentConfig_DoesNothing() {
        Map<Long, ScheduledFuture<?>> scheduledTasks = 
            (Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(scheduler, "scheduledTasks");
        assertNotNull(scheduledTasks);
        scheduledTasks.clear();

        assertDoesNotThrow(() -> scheduler.cancelConfig(999L));
        assertTrue(scheduledTasks.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void cancelConfig_WithExistentConfig_CancelsFuture() {
        Map<Long, ScheduledFuture<?>> scheduledTasks = 
            (Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(scheduler, "scheduledTasks");
        assertNotNull(scheduledTasks);

        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        scheduledTasks.put(50L, future);

        scheduler.cancelConfig(50L);

        verify(future).cancel(false);
        assertFalse(scheduledTasks.containsKey(50L));
    }
}
