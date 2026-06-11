package com.veritas.backend.integrations.jira.service;

import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import com.veritas.backend.requisition.entity.Request;

public interface JiraSyncService {
    void runManualSync(Long configId);
    void runAllSyncs();
    boolean testConnection(JiraConfigDto dto);
    void processQueue();
    void handleVeritasWorkflowChange(Request request);
    void postJiraComment(JiraConfig config, String issueKey, String commentText);
}
