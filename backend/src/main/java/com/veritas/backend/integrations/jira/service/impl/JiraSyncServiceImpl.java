package com.veritas.backend.integrations.jira.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veritas.backend.audit.service.impl.AuditServiceImpl;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.integrations.jira.dto.*;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import com.veritas.backend.integrations.jira.entity.JiraSyncQueueItem;
import com.veritas.backend.integrations.jira.mapper.JiraIssueMapper;
import com.veritas.backend.integrations.jira.repository.JiraConfigRepository;
import com.veritas.backend.integrations.jira.repository.JiraSyncQueueItemRepository;
import com.veritas.backend.integrations.jira.service.JiraSyncService;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.entity.Attachment;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestItem;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.requisition.repository.RequestItemRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.requisition.service.RequisitionService;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Base64;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
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

    private final ProjectRepository projectRepository;
    private final RequestItemRepository requestItemRepository;
    private final AttachmentRepository attachmentRepository;
    private final JiraSyncQueueItemRepository queueItemRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final InternalBudgetRepository internalBudgetRepository;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Autowired
    @Lazy
    private RequisitionService requisitionService;

    private static final DateTimeFormatter JIRA_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .optionalStart().appendOffset("+HH:MM", "Z").optionalEnd()
            .optionalStart().appendOffset("+HHMM", "Z").optionalEnd()
            .optionalStart().appendOffset("+HH", "Z").optionalEnd()
            .toFormatter();

    private static final Pattern QTY_UNIT_PATTERN =
            Pattern.compile("^(\\d+)\\s*(pcs|pc|x|units|unit|stk|packages|pkg|items|item)?$", Pattern.CASE_INSENSITIVE);

    @Override
    @Transactional
    public void runManualSync(Long configId) {
        JiraConfig config = configRepository.findById(configId)
            .orElseThrow(() -> new RuntimeException("Config not found"));

        try {
            syncConfig(config);
            config.setLastSyncTime(LocalDateTime.now());
            configRepository.save(config);
        } catch (RuntimeException e) {
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
        } catch (RestClientException e) {
            log.warn("Test connection failed for Jira config", e);
            return false;
        }
    }

    private void syncConfig(JiraConfig config) {
        String fieldId = config.getCustomFieldId();
        String jqlField = fieldId.startsWith("customfield_")
            ? "cf[" + fieldId.substring("customfield_".length()) + "]"
            : "cf[" + fieldId + "]";

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
        } catch (RestClientException e) {
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

        if (issueRecord.fields() != null && issueRecord.fields().description() != null) {
            request.setDescription(mapDescription(issueRecord.fields().description()));
        }

        if (issueRecord.fields() != null && issueRecord.fields().project() != null) {
            JiraProjectRecord jiraProj = issueRecord.fields().project();
            Optional<Project> matchedProject = Optional.empty();
            if (jiraProj.key() != null && !jiraProj.key().isBlank()) {
                matchedProject = projectRepository.findByProjectKey(jiraProj.key());
            }
            if (matchedProject.isEmpty() && jiraProj.name() != null && !jiraProj.name().isBlank()) {
                matchedProject = projectRepository.findByName(jiraProj.name());
            }

            if (matchedProject.isPresent()) {
                Project project = matchedProject.get();
                request.setProjectID(project);
                if (request.getRequestKey() == null) {
                    project.setRequestCounter(project.getRequestCounter() + 1);
                    projectRepository.saveAndFlush(project);
                    request.setRequestKey(project.getProjectKey() + "-" + project.getRequestCounter());
                }
            } else if (config.getFallbackProject() != null) {
                Project fallbackProject = config.getFallbackProject();
                request.setProjectID(fallbackProject);
                if (request.getRequestKey() == null) {
                    fallbackProject.setRequestCounter(fallbackProject.getRequestCounter() + 1);
                    projectRepository.saveAndFlush(fallbackProject);
                    request.setRequestKey(fallbackProject.getProjectKey() + "-" + fallbackProject.getRequestCounter());
                }
            }
        }

        if (request.getRequestKey() == null) {
            request.setRequestKey(key);
        }

        if (request.getWorkflowDefinitionID() == null) {
            if (config.getFallbackWorkflow() != null) {
                request.setWorkflowDefinitionID(config.getFallbackWorkflow());
            }
        }

        if (request.getCurrentStepID() == null && request.getWorkflowDefinitionID() != null) {
            workflowStepRepository.findFirstByWorkflowDefinitionAndWorkflowComponent(
                    request.getWorkflowDefinitionID(), WorkflowComponent.START_EVENT
            ).ifPresent(request::setCurrentStepID);
        }

        if (request.getUserID() == null && issueRecord.fields() != null && issueRecord.fields().reporter() != null) {
            String email = issueRecord.fields().reporter().emailAddress();
            if (email != null && !email.isBlank()) {
                Optional<User> matchedUser = userRepository.findByEmail(email.toLowerCase().trim());
                if (matchedUser.isPresent()) {
                    User user = matchedUser.get();
                    request.setUserID(user);
                    request.setTeamID(user.getTeam());
                }
            }
        }
        
        if (request.getUserID() == null && config.getFallbackUser() != null) {
            request.setUserID(config.getFallbackUser());
            request.setTeamID(config.getFallbackUser().getTeam());
        }

        if (request.getBudgetID() == null) {
            InternalBudget budget = new InternalBudget();
            budget.setBudgetName("Request: " + request.getRequestName());
            budget.setTotalAmount(BigDecimal.ZERO);
            if (request.getProjectID() != null) {
                budget.setParentBudget(request.getProjectID().getInternalBudget());
            }
            internalBudgetRepository.save(budget);

            request.setBudgetID(budget);
        }

        request = requestRepository.saveAndFlush(request);

        syncLineItems(request, config, issueRecord);

        syncAttachments(request, config, issueRecord);

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

            JiraSyncQueueItem lockItem = JiraSyncQueueItem.builder()
                    .request(request)
                    .jiraIssueKey(key)
                    .actionType("LOCK")
                    .status("PENDING")
                    .build();
            queueItemRepository.save(lockItem);

            User actor = null;
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User user) {
                actor = user;
            }
            auditService.createJiraSyncLog(actor, request, "Synced from Jira issue " + key + " | Created in Jira: " + offsetDateTime.toLocalDateTime());
        } else {
            log.warn("Failed to set Jira custom field for {}, syncing may repeat", key);
        }
    }

    private void syncAttachments(Request request, JiraConfig config, JiraIssueRecord issueRecord) {
        if (issueRecord.fields() == null || issueRecord.fields().attachment() == null) {
            return;
        }

        for (JiraAttachmentRecord attachment : issueRecord.fields().attachment()) {
            try {
                String contentUrl = attachment.content();
                HttpHeaders headers = createHeaders(config.getUsername(), config.getApiToken());
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                ResponseEntity<Resource> response =
                        restTemplate.exchange(contentUrl, HttpMethod.GET, entity, Resource.class);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    try (java.io.InputStream is = response.getBody().getInputStream()) {
                        requisitionService.saveAttachmentFromInputStream(
                                request.getRequestID(),
                                attachment.filename(),
                                attachment.mimeType(),
                                attachment.size(),
                                is
                        );
                    }
                }
            } catch (IOException | RuntimeException e) {
                log.warn("Failed to download attachment {} from Jira issue {}", attachment.filename(), issueRecord.key(), e);
            }
        }
    }

    private void syncLineItems(Request request, JiraConfig config, JiraIssueRecord issueRecord) {
        if (request.getItems() != null) {
            requestItemRepository.deleteAll(request.getItems());
            request.getItems().clear();
        } else {
            request.setItems(new ArrayList<>());
        }

        List<RequestItem> items = new ArrayList<>();

        if (issueRecord.fields() != null && issueRecord.fields().description() != null) {
            extractLineItemsFromDescriptionTable(issueRecord.fields().description(), request, items);
        }

        if (!items.isEmpty()) {
            requestItemRepository.saveAll(items);
            request.getItems().addAll(items);

            int totalQty = items.stream().mapToInt(RequestItem::getQuantity).sum();
            request.setTotalQuantity(totalQty);
            requestRepository.saveAndFlush(request);
        }
    }



    private void extractLineItemsFromDescriptionTable(JsonNode description, Request request, List<RequestItem> items) {
        if (description == null || description.isNull()) {
            return;
        }

        List<JsonNode> tableNodes = new ArrayList<>();
        findTableNodes(description, tableNodes);

        for (JsonNode tableNode : tableNodes) {
            JsonNode content = tableNode.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }

            for (JsonNode rowNode : content) {
                if (rowNode == null || !"tableRow".equals(rowNode.path("type").asText())) {
                    continue;
                }

                JsonNode rowContent = rowNode.get("content");
                if (rowContent == null || !rowContent.isArray() || rowContent.isEmpty()) {
                    continue;
                }

                boolean isHeader = false;
                for (JsonNode cell : rowContent) {
                    if ("tableHeader".equals(cell.path("type").asText())) {
                        isHeader = true;
                        break;
                    }
                }
                if (isHeader) {
                    continue;
                }

                String col1 = rowContent.size() > 0 ? extractTextFromNode(rowContent.get(0)) : "";
                String col2 = rowContent.size() > 1 ? extractTextFromNode(rowContent.get(1)) : "";
                String col3 = rowContent.size() > 2 ? extractTextFromNode(rowContent.get(2)) : "";
                String col4 = rowContent.size() > 3 ? extractTextFromNode(rowContent.get(3)) : "";

                if (col1.isEmpty()) {
                    continue;
                }

                RequestItem item = new RequestItem();
                item.setRequest(request);
                item.setName(col1);

                if (rowContent.size() == 2) {
                    parseQtyAndUnit(col2, item);
                } else if (rowContent.size() >= 3) {
                    try {
                        item.setQuantity(Integer.parseInt(col2.replaceAll("[^\\d]", "")));
                    } catch (NumberFormatException e) {
                        item.setQuantity(1);
                    }
                    item.setUnit(col3.isBlank() ? "pcs" : col3);
                } else {
                    item.setQuantity(1);
                    item.setUnit("pcs");
                }
                
                if (!col4.isBlank()) {
                    item.setDescription(col4);
                }

                items.add(item);
            }
        }
    }

    private void findTableNodes(JsonNode node, List<JsonNode> tableNodes) {
        if (node == null) return;
        if (node.isObject()) {
            if ("table".equals(node.path("type").asText())) {
                tableNodes.add(node);
            } else {
                if (node.has("content")) {
                    findTableNodes(node.get("content"), tableNodes);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                findTableNodes(child, tableNodes);
            }
        }
    }

    private String extractTextFromNode(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        extractText(node, sb);
        return sb.toString().trim();
    }

    private void parseQtyAndUnit(String text, RequestItem item) {
        if (text == null || text.isBlank()) {
            item.setQuantity(1);
            item.setUnit("pcs");
            return;
        }
        Matcher m = QTY_UNIT_PATTERN.matcher(text.trim());
        if (m.matches()) {
            item.setQuantity(Integer.parseInt(m.group(1)));
            String unit = m.group(2);
            item.setUnit(unit != null && !unit.isBlank() ? unit : "pcs");
        } else {
            try {
                String digitsOnly = text.replaceAll("[^\\d]", "");
                if (!digitsOnly.isEmpty()) {
                    item.setQuantity(Integer.parseInt(digitsOnly));
                } else {
                    item.setQuantity(1);
                }
            } catch (NumberFormatException e) {
                item.setQuantity(1);
            }
            item.setUnit("pcs");
        }
    }

    public String mapDescription(JsonNode description) {
        if (description == null || description.isNull()) {
            return null;
        }
        if (description.isTextual()) {
            return description.asText();
        }
        StringBuilder sb = new StringBuilder();
        extractText(description, sb);
        String text = sb.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private void extractText(JsonNode node, StringBuilder sb) {
        if (node == null) return;
        if (node.isObject()) {
            if (node.has("type") && "table".equals(node.get("type").asText())) {
                return;
            }
            if (node.has("type") && "text".equals(node.get("type").asText()) && node.has("text")) {
                sb.append(node.get("text").asText());
            } else {
                if (node.has("type") && "paragraph".equals(node.get("type").asText()) && sb.length() > 0) {
                    sb.append("\n");
                }
                if (node.has("content")) {
                    extractText(node.get("content"), sb);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                extractText(child, sb);
            }
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
        } catch (RestClientException e) {
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

    @Override
    @Transactional
    public void runAllSyncs() {
        log.info("Starting global sync for all Jira configurations");
        List<JiraConfig> allConfigs = configRepository.findAll();

        for (JiraConfig config : allConfigs) {
            try {
                syncConfig(config);
                config.setLastSyncTime(LocalDateTime.now());
                configRepository.save(config);
            } catch (RuntimeException e) {
                log.error("Error during global sync for config ID {}", config.getId(), e);
            }
        }
        log.info("Completed global sync for {} configurations", allConfigs.size());
    }

    @Override
    @Transactional
    public void handleVeritasWorkflowChange(Request request) {
        if (request.getJiraIssueKey() == null || request.getJiraIssueKey().isBlank()) {
            return;
        }

        JiraSyncQueueItem queueItem = JiraSyncQueueItem.builder()
                .request(request)
                .jiraIssueKey(request.getJiraIssueKey())
                .actionType("SYNC_JIRA")
                .status("PENDING")
                .build();

        queueItemRepository.save(queueItem);
        log.info("Queued SYNC_JIRA task for request: {}", request.getJiraIssueKey());
    }

    @Override
    @Transactional
    public void processQueue() {
        List<JiraSyncQueueItem> pendingItems = queueItemRepository.findByStatus("PENDING");
        if (pendingItems.isEmpty()) {
            return;
        }

        log.info("Processing {} pending Jira sync queue items", pendingItems.size());
        List<JiraConfig> allConfigs = configRepository.findAll();

        for (JiraSyncQueueItem item : pendingItems) {
            processQueueItem(item, allConfigs);
        }
    }

    private void processQueueItem(JiraSyncQueueItem item, List<JiraConfig> allConfigs) {
        item.setLastAttempt(LocalDateTime.now());
        item.setRetries(item.getRetries() + 1);

        try {
            Request request = item.getRequest();
            String issueUrl = request.getJiraIssueUrl();
            if (issueUrl == null || issueUrl.isBlank()) {
                log.warn("Jira issue URL is blank for request ID {}. Cannot process queue item.", request.getRequestID());
                item.setStatus("FAILED");
                queueItemRepository.save(item);
                return;
            }

            JiraConfig matchedConfig = null;
            for (JiraConfig config : allConfigs) {
                String cleanUrl = config.getJiraUrl().replaceAll("/+$", "");
                if (issueUrl.contains(cleanUrl)) {
                    matchedConfig = config;
                    break;
                }
            }

            if (matchedConfig == null) {
                log.warn("No matching Jira configuration found for issue URL: {}", issueUrl);
                if (item.getRetries() >= 5) {
                    item.setStatus("FAILED");
                }
                queueItemRepository.save(item);
                return;
            }

            boolean success = false;
            if ("LOCK".equals(item.getActionType())) {
                success = lockJiraIssue(matchedConfig, item.getJiraIssueKey(), request);
            } else if ("SYNC_JIRA".equals(item.getActionType())) {
                success = syncVeritasToJira(matchedConfig, request);
            }

            if (success) {
                item.setStatus("COMPLETED");
                log.info("Successfully processed queue item {} of type {}", item.getId(), item.getActionType());
            } else {
                if (item.getRetries() >= 5) {
                    item.setStatus("FAILED");
                    log.warn("Jira sync queue item {} failed after max retries", item.getId());
                } else {
                    item.setStatus("PENDING");
                }
            }
        } catch (RuntimeException e) {
            log.error("Error processing queue item {}", item.getId(), e);
            if (item.getRetries() >= 5) {
                item.setStatus("FAILED");
            } else {
                item.setStatus("PENDING");
            }
        }

        queueItemRepository.save(item);
    }

    private boolean lockJiraIssue(JiraConfig config, String issueKey, Request request) {
        boolean transitioned = transitionJiraIssue(config, issueKey, "Delegated Waiting", true);
        try {
            postJiraComment(config, issueKey, "This requisition has been imported to Veritas and is locked in Jira. Please proceed with all approvals and edits in Veritas.");
        } catch (RestClientException e) {
            log.warn("Failed to add locking comment to Jira for issue {}", issueKey, e);
        }
        createJiraRemoteLink(config, issueKey, request);
        return transitioned;
    }
    
    private void createJiraRemoteLink(JiraConfig config, String issueKey, Request request) {
        String url = config.getJiraUrl  ().replaceAll("/+$", "") + "/rest/api/3/issue/" + issueKey + "/remotelink";
        HttpHeaders headers = createHeaders(config.getUsername(), config.getApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String linkUrl = frontendUrl.replaceAll("/+$", "") + "/requisitions/" + request.getRequestID();
        String title = request.getRequestName() != null && !request.getRequestName().isBlank() ? request.getRequestName() : request.getRequestKey();
        
        String jsonPayload = String.format("{\"object\": {\"url\": \"%s\", \"title\": \"%s\"}}", linkUrl, title);
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
        
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (RestClientException e) {
            log.warn("Failed to create remote link in Jira for issue {}", issueKey, e);
        }
    }

    private boolean syncVeritasToJira(JiraConfig config, Request request) {
        String key = request.getJiraIssueKey();

        boolean updateDescriptionSuccess = false;
        try {
            String updateUrl = config.getJiraUrl().replaceAll("/+$", "") + "/rest/api/3/issue/" + key;
            HttpHeaders headers = createHeaders(config.getUsername(), config.getApiToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode updatePayload = mapper.createObjectNode();
            ObjectNode fieldsNode = mapper.createObjectNode();
            fieldsNode.set("description", buildJiraDescriptionPayload(request));
            updatePayload.set("fields", fieldsNode);

            HttpEntity<String> entity = new HttpEntity<>(updatePayload.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
            updateDescriptionSuccess = response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.error("Failed to update Jira description/table for issue {}", key, e);
        }

        boolean transitionSuccess = false;
        String targetStepName = null;
        if (RequestStatus.FINISHED.equals(request.getState())) {
            targetStepName = "Delegated Ready";
        } else if (request.getCurrentStepID() != null) {
            targetStepName = request.getCurrentStepID().getName();
        }
        
        if (targetStepName != null && !targetStepName.isBlank()) {
            transitionSuccess = transitionJiraIssue(config, key, targetStepName, false);
        } else {
            transitionSuccess = true;
        }
        
        String stepNameForComment;
        if (RequestStatus.FINISHED.equals(request.getState())) {
            stepNameForComment = "Finished";
        } else if (request.getCurrentStepID() != null && request.getCurrentStepID().getName() != null) {
            stepNameForComment = request.getCurrentStepID().getName();
        } else {
            stepNameForComment = "Unknown";
        }

        String requestName = request.getRequestName() != null && !request.getRequestName().isBlank() ? request.getRequestName() : request.getRequestKey();
        try {
            postStatusChangeComment(config, key, requestName, stepNameForComment);
        } catch (RestClientException e) {
            log.warn("Failed to post status comment to Jira for {}", key, e);
        }

        syncAttachmentsToJira(config, key, request);

        return updateDescriptionSuccess || transitionSuccess;
    }

    private void syncAttachmentsToJira(JiraConfig config, String issueKey, Request request) {
        String issueUrl = config.getJiraUrl().replaceAll("/+$", "") + "/rest/api/3/issue/" + issueKey + "?fields=attachment";
        HttpHeaders headers = createHeaders(config.getUsername(), config.getApiToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.exchange(issueUrl, HttpMethod.GET, entity, JsonNode.class);
        } catch (RestClientException e) {
            log.warn("Failed to fetch Jira issue attachments for issue {}", issueKey, e);
            return;
        }

        if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return;
        }

        JsonNode fieldsNode = response.getBody().get("fields");
        if (fieldsNode == null || !fieldsNode.has("attachment")) {
            return;
        }

        JsonNode attachmentsNode = fieldsNode.get("attachment");
        List<String> jiraAttachmentNames = new ArrayList<>();
        java.util.Map<String, String> jiraAttachmentIdByName = new java.util.HashMap<>();
        if (attachmentsNode.isArray()) {
            for (JsonNode attachment : attachmentsNode) {
                String name = attachment.get("filename").asText();
                String id = attachment.get("id").asText();
                jiraAttachmentNames.add(name);
                jiraAttachmentIdByName.put(name, id);
            }
        }

        List<Attachment> veritasAttachments = attachmentRepository.findByRequest(request);
        List<String> veritasAttachmentNames = new ArrayList<>();

        for (Attachment vAtt : veritasAttachments) {
            String vName = vAtt.getFileName();
            veritasAttachmentNames.add(vName);
            if (!jiraAttachmentNames.contains(vName)) {
                uploadAttachmentToJira(config, issueKey, vAtt);
            }
        }

        for (String jName : jiraAttachmentNames) {
            if (!veritasAttachmentNames.contains(jName)) {
                deleteAttachmentFromJira(config, jiraAttachmentIdByName.get(jName));
            }
        }
    }

    private void uploadAttachmentToJira(JiraConfig config, String issueKey, Attachment attachment) {
        String uploadUrl = config.getJiraUrl().replaceAll("/+$", "") + "/rest/api/3/issue/" + issueKey + "/attachments";
        HttpHeaders headers = createHeaders(config.getUsername(), config.getApiToken());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Atlassian-Token", "no-check");

        try {
            Path path = Paths.get(attachment.getStoragePath());
            if (!Files.exists(path)) {
                log.warn("Attachment file not found on disk: {}", path);
                return;
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            Resource resource = new FileSystemResource(path.toFile());
            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, String.class);
            log.info("Uploaded attachment {} to Jira issue {}", attachment.getFileName(), issueKey);
        } catch (RestClientException e) {
            log.error("Failed to upload attachment {} to Jira issue {}", attachment.getFileName(), issueKey, e);
        }
    }

    private void deleteAttachmentFromJira(JiraConfig config, String attachmentId) {
        String deleteUrl = config.getJiraUrl().replaceAll("/+$", "") + "/rest/api/3/attachment/" + attachmentId;
        HttpHeaders headers = createHeaders(config.getUsername(), config.getApiToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, Void.class);
            log.info("Deleted attachment {} from Jira", attachmentId);
        } catch (RestClientException e) {
            log.error("Failed to delete attachment {} from Jira", attachmentId, e);
        }
    }

    private boolean transitionJiraIssue(JiraConfig config, String issueKey, String targetName, boolean isLock) {
        String transitionsUrl = config.getJiraUrl().replaceAll("/+$", "") + "/rest/api/3/issue/" + issueKey + "/transitions";
        HttpHeaders headers = createHeaders(config.getUsername(), config.getApiToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.exchange(transitionsUrl, HttpMethod.GET, entity, JsonNode.class);
        } catch (RestClientException e) {
            log.warn("Failed to fetch available transitions for Jira issue {}", issueKey, e);
            return false;
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return false;
        }

        JsonNode body = response.getBody();
        JsonNode transitionsNode = body.get("transitions");
        if (transitionsNode == null || !transitionsNode.isArray()) {
            return false;
        }

        String transitionId = null;
        for (JsonNode t : transitionsNode) {
            String name = t.has("name") ? t.get("name").asText() : "";
            String toName = "";
            if (t.has("to") && t.get("to").has("name")) {
                toName = t.get("to").get("name").asText();
            }

            boolean match = false;
            if (isLock) {
                String nLower = name.toLowerCase();
                String toLower = toName.toLowerCase();
                if (nLower.equals("delegated waiting") || toLower.equals("delegated waiting")) {
                    match = true;
                }
            } else {
                if (name.equalsIgnoreCase(targetName) || toName.equalsIgnoreCase(targetName) ||
                    name.toLowerCase().contains(targetName.toLowerCase()) || toName.toLowerCase().contains(targetName.toLowerCase())) {
                    match = true;
                }
            }

            if (match) {
                transitionId = t.get("id").asText();
                break;
            }
        }

        if (transitionId == null) {
            log.warn("No matching transition found in Jira for issue {} and target matching '{}'", issueKey, targetName);
            return isLock;
        }

        headers.setContentType(MediaType.APPLICATION_JSON);
        String transitionPayload = String.format("{\"transition\": {\"id\": \"%s\"}}", transitionId);
        HttpEntity<String> transitionEntity = new HttpEntity<>(transitionPayload, headers);

        try {
            ResponseEntity<String> transitionResponse = restTemplate.exchange(transitionsUrl, HttpMethod.POST, transitionEntity, String.class);
            return transitionResponse.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.error("Failed to execute transition ID {} on Jira issue {}", transitionId, issueKey, e);
            return false;
        }
    }

    private void postJiraComment(JiraConfig config, String issueKey, String commentText) {
        String url = config.getJiraUrl().replaceAll("/+$", "") + "/rest/api/3/issue/" + issueKey + "/comment";
        HttpHeaders headers = createHeaders(config.getUsername(), config.getApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String cleanComment = commentText != null ? commentText.replace("\\", "\\\\")
                                                               .replace("\"", "\\\"")
                                                               .replace("\n", "\\n")
                                                               .replace("\r", "") : "";
        String jsonPayload = String.format(
            "{\"body\": {\"type\": \"doc\", \"version\": 1, \"content\": [{\"type\": \"paragraph\", \"content\": [{\"type\": \"text\", \"text\": \"%s\"}]}]}}",
            cleanComment
        );

        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    private void postStatusChangeComment(JiraConfig config, String issueKey, String requestName, String stepName) {
        String url = config.getJiraUrl().replaceAll("/+$", "") + "/rest/api/3/issue/" + issueKey + "/comment";
        HttpHeaders headers = createHeaders(config.getUsername(), config.getApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String cleanReqName = requestName != null ? requestName.replace("\"", "\\\"") : "";
        String cleanStepName = stepName != null ? stepName.replace("\"", "\\\"") : "";
        
        String jsonPayload = String.format(
            "{\"body\": {\"type\": \"doc\", \"version\": 1, \"content\": [{\"type\": \"paragraph\", \"content\": [" +
            "{\"type\": \"text\", \"text\": \"Request \"}, " +
            "{\"type\": \"text\", \"text\": \"%s\", \"marks\": [{\"type\": \"strong\"}]}, " +
            "{\"type\": \"text\", \"text\": \" moved to state \"}, " +
            "{\"type\": \"text\", \"text\": \"%s\", \"marks\": [{\"type\": \"strong\"}]}" +
            "]}]}}",
            cleanReqName, cleanStepName
        );

        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    public JsonNode buildJiraDescriptionPayload(Request request) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode docNode = mapper.createObjectNode();
        docNode.put("type", "doc");
        docNode.put("version", 1);

        ArrayNode contentNode = mapper.createArrayNode();

        String desc = request.getDescription();
        if (desc == null || desc.isBlank()) {
            desc = "No additional description provided.";
        }

        ObjectNode descParagraph = mapper.createObjectNode();
        descParagraph.put("type", "paragraph");
        ArrayNode descTextArray = mapper.createArrayNode();
        ObjectNode descTextNode = mapper.createObjectNode();
        descTextNode.put("type", "text");
        descTextNode.put("text", desc);
        descTextArray.add(descTextNode);
        descParagraph.set("content", descTextArray);
        contentNode.add(descParagraph);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            ObjectNode headingParagraph = mapper.createObjectNode();
            headingParagraph.put("type", "paragraph");
            ArrayNode headingTextArray = mapper.createArrayNode();
            ObjectNode headingTextNode = mapper.createObjectNode();
            headingTextNode.put("type", "text");
            headingTextNode.put("text", "\nLine Items (Synced from Veritas):");
            
            ArrayNode marksArray = mapper.createArrayNode();
            ObjectNode boldMark = mapper.createObjectNode();
            boldMark.put("type", "strong");
            marksArray.add(boldMark);
            headingTextNode.set("marks", marksArray);

            headingTextArray.add(headingTextNode);
            headingParagraph.set("content", headingTextArray);
            contentNode.add(headingParagraph);

            ObjectNode tableNode = mapper.createObjectNode();
            tableNode.put("type", "table");
            ArrayNode rowsArray = mapper.createArrayNode();

            ObjectNode headerRow = mapper.createObjectNode();
            headerRow.put("type", "tableRow");
            ArrayNode headerCells = mapper.createArrayNode();
            headerCells.add(createTableCellNode(mapper, "tableHeader", "Description"));
            headerCells.add(createTableCellNode(mapper, "tableHeader", "Quantity"));
            headerCells.add(createTableCellNode(mapper, "tableHeader", "Unit"));
            headerCells.add(createTableCellNode(mapper, "tableHeader", "Detailed Description"));
            headerRow.set("content", headerCells);
            rowsArray.add(headerRow);

            for (RequestItem item : request.getItems()) {
                ObjectNode dataRow = mapper.createObjectNode();
                dataRow.put("type", "tableRow");
                ArrayNode dataCells = mapper.createArrayNode();
                dataCells.add(createTableCellNode(mapper, "tableCell", item.getName()));
                dataCells.add(createTableCellNode(mapper, "tableCell", String.valueOf(item.getQuantity())));
                dataCells.add(createTableCellNode(mapper, "tableCell", item.getUnit() != null ? item.getUnit() : "pcs"));
                dataCells.add(createTableCellNode(mapper, "tableCell", item.getDescription() != null ? item.getDescription() : ""));
                dataRow.set("content", dataCells);
                rowsArray.add(dataRow);
            }

            tableNode.set("content", rowsArray);
            contentNode.add(tableNode);
        }

        docNode.set("content", contentNode);
        return docNode;
    }

    private ObjectNode createTableCellNode(ObjectMapper mapper, String type, String text) {
        ObjectNode cell = mapper.createObjectNode();
        cell.put("type", type);
        ArrayNode cellContent = mapper.createArrayNode();
        ObjectNode p = mapper.createObjectNode();
        p.put("type", "paragraph");
        ArrayNode pContent = mapper.createArrayNode();
        ObjectNode t = mapper.createObjectNode();
        t.put("type", "text");
        t.put("text", text);
        pContent.add(t);
        p.set("content", pContent);
        cellContent.add(p);
        cell.set("content", cellContent);
        return cell;
    }
}
