package com.veritas.backend.integrations.jira.service;

import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import java.util.List;

public interface JiraConfigService {
    List<JiraConfigDto> getAllConfigs();

    JiraConfigDto getConfigById(Long id);

    JiraConfigDto createConfig(JiraConfigDto dto);

    JiraConfigDto updateConfig(Long id, JiraConfigDto dto);

}
