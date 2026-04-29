package com.veritas.backend.integrations.jira.service;

import com.veritas.backend.integrations.jira.dto.JiraConfigDto;

public interface JiraSyncService {
    void runManualSync(Long configId);

    boolean testConnection(JiraConfigDto dto);
}
