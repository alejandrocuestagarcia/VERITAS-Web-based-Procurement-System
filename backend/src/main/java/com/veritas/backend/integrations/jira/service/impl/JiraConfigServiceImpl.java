package com.veritas.backend.integrations.jira.service.impl;

import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.dto.JiraConfigResponseDto;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import com.veritas.backend.integrations.jira.mapper.JiraConfigMapper;
import com.veritas.backend.integrations.jira.repository.JiraConfigRepository;
import com.veritas.backend.integrations.jira.service.DynamicJiraScheduler;
import com.veritas.backend.integrations.jira.service.JiraConfigService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
            throw new RuntimeException("A configuration with this Jira URL and JQL already exists.");
        }
        if (dto.apiToken() == null || dto.apiToken().isBlank()) {
            throw new RuntimeException("API Token is required for new configurations.");
        }
        JiraConfig entity = mapper.toEntity(dto);
        JiraConfig saved = repository.save(entity);
        scheduler.scheduleConfig(saved);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public JiraConfigResponseDto updateConfig(Long id, JiraConfigDto dto) {
        repository.findByJiraUrlAndJql(dto.jiraUrl(), dto.jql()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("Another configuration already uses this Jira URL and JQL.");
            }
        });

        JiraConfig entity = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Config not found"));

        String existingToken = entity.getApiToken();
        mapper.updateEntityFromDto(dto, entity);

        // Preserve existing token if the incoming DTO has no token
        if (dto.apiToken() == null || dto.apiToken().trim().isEmpty()) {
            entity.setApiToken(existingToken);
        }

        JiraConfig updated = repository.save(entity);
        scheduler.scheduleConfig(updated);
        return mapper.toDto(updated);
    }

}
