package com.veritas.backend.integrations.jira.service;

import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import com.veritas.backend.requisition.entity.Request;

public interface JiraSyncService {

    /**
     * Runs a full Jira sync for a single configuration, fetching unprocessed issues
     * matching its JQL and importing them as requests. Updates the last sync timestamp on success.
     *
     * @param configId the ID of the {@link JiraConfig} to sync
     * @throws RuntimeException if no config exists with the given ID
     */
    void runManualSync(Long configId);

    /**
     * Runs a full Jira sync for all existing configurations.
     * Errors in individual configs are logged and do not interrupt the remaining syncs.
     */
    void runAllSyncs();

    /**
     * Tests connectivity to a Jira instance using the provided configuration credentials.
     * If no API token is supplied but a config ID is present, the stored token is used as fallback.
     *
     * @param dto the Jira configuration to test
     * @return {@code true} if the connection succeeded, {@code false} otherwise
     */
    boolean testConnection(JiraConfigDto dto);

    /**
     * Processes all pending items in the Jira sync queue.
     * Each item is either a lock operation (transition issue and post comment) or
     * a Veritas-to-Jira sync (update description, transition, attachments, and post comment).
     * Items are retried up to 5 times before being marked as failed.
     */
    void processQueue();

    /**
     * Enqueues a Veritas-to-Jira sync for the given request following a workflow state change.
     * Does nothing if the request has no linked Jira issue or config.
     *
     * @param request the request whose workflow state changed
     */
    void handleVeritasWorkflowChange(Request request);

    /**
     * Posts a plain-text comment to a Jira issue using the Jira v3 document format.
     *
     * @param config the Jira configuration providing the URL and credentials
     * @param issueKey the key of the Jira issue to comment on
     * @param commentText the comment text to post
     */
    void postJiraComment(JiraConfig config, String issueKey, String commentText);
}
