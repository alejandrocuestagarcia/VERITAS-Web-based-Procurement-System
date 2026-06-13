package com.veritas.backend.integrations.jira.service;

import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.dto.JiraConfigResponseDto;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JiraConfigService {

    /**
     * Returns a paginated list of Jira configurations, optionally filtered by name.
     *
     * @param pageable pagination and sorting parameters
     * @param filter optional search string to filter configs by name; ignored if null or blank
     * @return a page of matching {@link JiraConfigResponseDto}
     */
    Page<JiraConfigResponseDto> getAllConfigs(Pageable pageable, String filter);

    /**
     * Returns a single Jira configuration by its ID.
     *
     * @param id the config ID
     * @return the matching {@link JiraConfigResponseDto}
     * @throws RuntimeException if no config exists with the given ID
     */
    JiraConfigResponseDto getConfigById(Long id);

    /**
     * Creates a new Jira configuration, registers it with the dynamic scheduler,
     * and validates fallback entity assignments.
     *
     * @param dto the configuration payload
     * @return the created {@link JiraConfigResponseDto}
     * @throws EntityExistsException if a config with the same Jira URL and JQL already exists
     * @throws IllegalArgumentException if the API token is missing, or fallback entities fail cross-team or cross-department validation
     * @throws EntityNotFoundException if any referenced fallback entity does not exist
     */
    JiraConfigResponseDto createConfig(JiraConfigDto dto);

    /**
     * Updates an existing Jira configuration and re-registers it with the dynamic scheduler.
     * If no API token is provided in the payload, the existing token is preserved.
     *
     * @param id the ID of the config to update
     * @param dto the updated configuration payload
     * @return the updated {@link JiraConfigResponseDto}
     * @throws EntityNotFoundException if the config or any fallback entity does not exist
     * @throws EntityExistsException if another config already uses the same Jira URL and JQL
     * @throws IllegalArgumentException if fallback entities fail cross-team or cross-department validation
     */
    JiraConfigResponseDto updateConfig(Long id, JiraConfigDto dto);

    /**
     * Deletes a Jira configuration, cancels its schedule, and unsyncs all linked requests.
     * For each unsynced request a Jira comment is posted and an audit log entry is created.
     *
     * @param id the ID of the config to delete
     * @return the deleted config as a {@link JiraConfigResponseDto}
     * @throws EntityNotFoundException if no config exists with the given ID
     */
    JiraConfigResponseDto deleteConfigById(Long id);
}
