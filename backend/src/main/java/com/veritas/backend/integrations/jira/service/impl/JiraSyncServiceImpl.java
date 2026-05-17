package com.veritas.backend.integrations.jira.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.veritas.backend.audit.service.impl.AuditServiceImpl;
import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.dto.JiraIssueRecord;
import com.veritas.backend.integrations.jira.dto.JiraSearchResponseRecord;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import com.veritas.backend.integrations.jira.mapper.JiraIssueMapper;
import com.veritas.backend.integrations.jira.repository.JiraConfigRepository;
import com.veritas.backend.integrations.jira.service.JiraSyncService;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.repository.RequestRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Base64;
import java.util.Optional;

import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class JiraSyncServiceImpl implements JiraSyncService {

    private final JiraConfigRepository configRepository;
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final AuditServiceImpl auditService;
    private final JiraIssueMapper issueMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final DateTimeFormatter JIRA_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .optionalStart().appendOffset("+HH:MM", "Z").optionalEnd()
            .optionalStart().appendOffset("+HHMM", "Z").optionalEnd()
            .optionalStart().appendOffset("+HH", "Z").optionalEnd()
            .toFormatter();

    @Override
    @Transactional
    public void runManualSync(Long configId) {
        JiraConfig config = configRepository.findById(configId)
            .orElseThrow(() -> new RuntimeException("Config not found"));

        try {
            syncConfig(config);
            config.setLastSyncTime(LocalDateTime.now());
            configRepository.save(config);
        } catch (Exception e) {
            log.error("Error during manual sync for config ID {}", configId, e);
        }
    }

    @Override
    public boolean testConnection(JiraConfigDto dto) {
        String token = dto.apiToken();
        if ((token == null || token.isBlank()) && dto.id() != null) {
            token = configRepository.findById(dto.id())
                .map(JiraConfig::getApiToken)
                .orElse(token);
        }

        if (dto.jiraUrl() == null || dto.username() == null || token == null || token.isBlank()) {
            log.warn("Test connection failed: missing required fields (URL, username, or token)");
            return false;
        }
        String url = dto.jiraUrl().replaceAll("/+$", "") + "/rest/api/3/myself";
        HttpHeaders headers = createHeaders(dto.username(), token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Test connection failed for Jira config", e);
            return false;
        }
    }

    private void syncConfig(JiraConfig config) {
        String fieldId = config.getCustomFieldId();
        String jqlField = fieldId.startsWith("customfield_")
            ? "cf[" + fieldId.substring("customfield_".length()) + "]"
            : fieldId;

        String appendedJql = String.format("%s AND %s IS EMPTY", config.getJql(), jqlField);

        String searchUrl =
            UriComponentsBuilder.fromUriString(config.getJiraUrl().replaceAll("/+$", ""))
                .path("/rest/api/3/search/jql")
                .queryParam("jql", appendedJql)
                .queryParam("fields", "*all")
                .build()
                .toUriString();

        HttpHeaders headers = createHeaders(config.getUsername(), config.getApiToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<JiraSearchResponseRecord> response;
        try {
            response = restTemplate.exchange(searchUrl, HttpMethod.GET, entity,
                JiraSearchResponseRecord.class);
        } catch (Exception e) {
            log.error("Failed to query Jira API with JQL: {}", appendedJql, e);
            return;
        }

        JiraSearchResponseRecord responseBody = response.getBody();
        if (responseBody == null || responseBody.issues() == null) {
            return;
        }

        for (JiraIssueRecord issue : responseBody.issues()) {
            processIssue(config, issue);
        }
    }

    private void processIssue(JiraConfig config, JiraIssueRecord issueRecord) {
        String key = issueRecord.key();
        Optional<Request> existingOpt = requestRepository.findByJiraIssueKey(key);
        Request request;

        if (existingOpt.isPresent()) {
            request = existingOpt.get();
            if ("SYNCED".equals(request.getJiraStatus())) {
                return; // Already processed
            }
        } else {
            request = issueMapper.toRequest(issueRecord);
        }

        String browserUrl = config.getJiraUrl().replaceAll("/+$", "") + "/browse/" + key;
        request.setJiraIssueUrl(browserUrl);

        if (request.getUserID() == null && issueRecord.fields() != null && issueRecord.fields().reporter() != null) {
            String email = issueRecord.fields().reporter().emailAddress();
            if (email != null && !email.isBlank()) {
                Optional<User> matchedUser = userRepository.findByEmail(email);
                if (matchedUser.isPresent()) {
                    User user = matchedUser.get();
                    request.setUserID(user);
                    request.setTeamID(user.getTeam());
                }
            }
        }

        request = requestRepository.saveAndFlush(request);

        boolean success = updateJiraCustomField(config, key, request.getRequestID().toString());

        if (success) {
            request.setJiraStatus("SYNCED");

            OffsetDateTime offsetDateTime = OffsetDateTime.parse(
                    issueRecord.fields().created(),
                    JIRA_DATE_FORMATTER
            );
            request.setCreatedAt(offsetDateTime.toLocalDateTime());

            requestRepository.saveAndFlush(request);
            log.info("Successfully synced Jira issue: {}", key);

            User actor = null;
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User user) {
                actor = user;
            }
            auditService.createJiraSyncLog(actor, request, "Synced from Jira issue " + key + " | Created in Jira: " + offsetDateTime.toLocalDateTime());
        } else {
            log.warn("Failed to update Jira custom field for issue: {}", key);
        }
    }

    private boolean updateJiraCustomField(JiraConfig config, String issueKey, String localId) {
        String updateUrl =
            config.getJiraUrl().replaceAll("/+$", "") + "/rest/api/3/issue/" + issueKey;

        HttpHeaders headers = createHeaders(config.getUsername(), config.getApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String fieldId = config.getCustomFieldId();
        String jsonField = fieldId.startsWith("customfield_") ? fieldId : "customfield_" + fieldId;

        String jsonPayload = String.format("{\"fields\": {\"%s\": \"%s\"}}", jsonField, localId);
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

        try {
            ResponseEntity<String> response =
                restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error updating Jira issue {} with local ID {}", issueKey, localId, e);
            return false;
        }
    }

    private HttpHeaders createHeaders(String username, String apiToken) {
        String auth = username + ":" + apiToken;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        headers.set("Accept", "application/json");
        return headers;
    }
}
