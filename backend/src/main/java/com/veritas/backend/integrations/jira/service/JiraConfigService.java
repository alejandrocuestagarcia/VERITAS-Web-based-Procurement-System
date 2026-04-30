package com.veritas.backend.integrations.jira.service;

import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.dto.JiraConfigResponseDto;
import java.util.List;

public interface JiraConfigService {
    List<JiraConfigResponseDto> getAllConfigs();

    JiraConfigResponseDto getConfigById(Long id);

    JiraConfigResponseDto createConfig(JiraConfigDto dto);

    JiraConfigResponseDto updateConfig(Long id, JiraConfigDto dto);

}
