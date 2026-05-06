package com.veritas.backend.integrations.jira.service;

import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.dto.JiraConfigResponseDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JiraConfigService {

    Page<JiraConfigResponseDto> getAllConfigs(Pageable pageable, String filter);

    JiraConfigResponseDto getConfigById(Long id);

    JiraConfigResponseDto createConfig(JiraConfigDto dto);

    JiraConfigResponseDto updateConfig(Long id, JiraConfigDto dto);

}
