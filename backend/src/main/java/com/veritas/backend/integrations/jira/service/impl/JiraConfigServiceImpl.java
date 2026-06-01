package com.veritas.backend.integrations.jira.service.impl;

import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.dto.JiraConfigResponseDto;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import com.veritas.backend.integrations.jira.mapper.JiraConfigMapper;
import com.veritas.backend.integrations.jira.repository.JiraConfigRepository;
import com.veritas.backend.integrations.jira.service.DynamicJiraScheduler;
import com.veritas.backend.integrations.jira.service.JiraConfigService;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.integrations.jira.repository.JiraSyncQueueItemRepository;
import com.veritas.backend.integrations.jira.entity.JiraSyncQueueItem;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.audit.service.AuditService;
import com.veritas.backend.user.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraConfigServiceImpl implements JiraConfigService {

    private final JiraConfigRepository repository;
    private final JiraConfigMapper mapper;
    private final DynamicJiraScheduler scheduler;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final JiraSyncQueueItemRepository queueItemRepository;
    private final RequestRepository requestRepository;
    private final AuditService auditService;

    @Override
    public Page<JiraConfigResponseDto> getAllConfigs(Pageable pageable, String filter) {
        log.debug("Fetching filtered users – filter: '{}', page: {}", filter, pageable.getPageNumber());
        String query = (filter != null && !filter.isBlank()) ? "%" + filter.trim().toLowerCase() + "%" : null;
        return repository.findAllFiltered(query, pageable).map(mapper::toDto);
    }

    @Override
    public JiraConfigResponseDto getConfigById(Long id) {
        JiraConfig entity = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Config not found"));
        return mapper.toDto(entity);
    }

    @Override
    @Transactional
    public JiraConfigResponseDto createConfig(JiraConfigDto dto) {
        if (repository.existsByJiraUrlAndJql(dto.jiraUrl(), dto.jql())) {
            throw new EntityExistsException("A configuration with this Jira URL and JQL already exists.");
        }
        if (dto.apiToken() == null || dto.apiToken().isBlank()) {
            throw new IllegalArgumentException("API Token is required for new configurations.");
        }
        JiraConfig entity = mapper.toEntity(dto);
        resolveFallbackEntities(dto, entity);
        
        if (dto.isActive() != null) {
            entity.setActive(dto.isActive());
        } else {
            entity.setActive(true);
        }

        JiraConfig saved = repository.save(entity);
        scheduler.scheduleConfig(saved);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public JiraConfigResponseDto deleteConfigById(Long id) {
        JiraConfig entity = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Config not found"));

        scheduler.cancelConfig(id);

        String cleanUrl = entity.getJiraUrl().replaceAll("/+$", "");
        List<JiraSyncQueueItem> pendingItems = queueItemRepository.findByStatus("PENDING");
        List<JiraSyncQueueItem> itemsToDelete = pendingItems.stream()
            .filter(item -> item.getRequest() != null && 
                            item.getRequest().getJiraIssueUrl() != null && 
                            item.getRequest().getJiraIssueUrl().contains(cleanUrl))
            .collect(Collectors.toList());
        if (!itemsToDelete.isEmpty()) {
            queueItemRepository.deleteAll(itemsToDelete);
        }

        User actor = null;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            actor = user;
        }

        List<Request> syncedRequests = requestRepository.findByJiraConfigId(id);
        for (Request request : syncedRequests) {
            request.setJiraStatus("NOT_SYNCED");
            request.setJiraConfig(null);
            auditService.createJiraUnsyncLog(actor, request, "Unsynced from Jira issue " + request.getJiraIssueKey() + " | Connection deleted");
        }
        if (!syncedRequests.isEmpty()) {
            requestRepository.saveAll(syncedRequests);
        }

        repository.delete(entity);
        return mapper.toDto(entity);
    }

    @Override
    @Transactional
    public JiraConfigResponseDto updateConfig(Long id, JiraConfigDto dto) {
        repository.findByJiraUrlAndJql(dto.jiraUrl(), dto.jql()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new EntityExistsException("Another configuration already uses this Jira URL and JQL.");
            }
        });

        JiraConfig entity = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Config not found"));

        String existingToken = entity.getApiToken();
        mapper.updateEntityFromDto(dto, entity);

        // Preserve existing token if the incoming DTO has no token
        if (dto.apiToken() == null || dto.apiToken().trim().isEmpty()) {
            entity.setApiToken(existingToken);
        }
        resolveFallbackEntities(dto, entity);

        if (dto.isActive() != null) {
            entity.setActive(dto.isActive());
        }

        JiraConfig updated = repository.save(entity);
        scheduler.scheduleConfig(updated);
        return mapper.toDto(updated);
    }

    private void resolveFallbackEntities(JiraConfigDto dto, JiraConfig entity) {
        if (dto.fallbackUserId() != null) {
            entity.setFallbackUser(userRepository.findById(dto.fallbackUserId())
                .orElseThrow(() -> new DataIntegrityViolationException("Fallback user not found")));
        } else {
            entity.setFallbackUser(null);
        }
        
        if (dto.fallbackProjectId() != null) {
            entity.setFallbackProject(projectRepository.findById(dto.fallbackProjectId())
                .orElseThrow(() -> new DataIntegrityViolationException("Fallback project not found")));
        } else {
            entity.setFallbackProject(null);
        }
        
        if (dto.fallbackWorkflowId() != null) {
            entity.setFallbackWorkflow(workflowDefinitionRepository.findById(dto.fallbackWorkflowId())
                .orElseThrow(() -> new DataIntegrityViolationException("Fallback workflow not found")));
        } else {
            entity.setFallbackWorkflow(null);
        }
    }

}
