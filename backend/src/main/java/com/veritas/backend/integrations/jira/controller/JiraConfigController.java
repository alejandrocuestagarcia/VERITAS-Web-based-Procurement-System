package com.veritas.backend.integrations.jira.controller;

import com.veritas.backend.audit.dto.AuditLogDto;
import com.veritas.backend.audit.service.impl.AuditServiceImpl;
import com.veritas.backend.config.annotations.IsAdministrator;
import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.dto.JiraConfigResponseDto;
import com.veritas.backend.integrations.jira.service.JiraConfigService;
import com.veritas.backend.integrations.jira.service.JiraSyncService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;

import static com.veritas.backend.common.model.AuditActionConstants.JIRA_SYNC;

@Slf4j
@RestController
@RequestMapping(value = "/jira-configs", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@IsAdministrator
public class JiraConfigController {

    private final JiraConfigService service;
    private final JiraSyncService syncService;
    private final AuditServiceImpl auditService;

    @Operation(summary = "List jira configs", description = "Retrieves all jira configs.")
    @GetMapping
    public ResponseEntity<Page<JiraConfigResponseDto>> getAllConfigs(Pageable pageable, @RequestParam(required = false)String search) {
        log.info("GET /jira-congigs – page: {}, size: {}, search: '{}'", pageable.getPageNumber(), pageable.getPageSize(), search);
        return ResponseEntity.ok(service.getAllConfigs(pageable, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JiraConfigResponseDto> getConfigById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getConfigById(id));
    }

    @PostMapping
    public ResponseEntity<JiraConfigResponseDto> createConfig(@Valid @RequestBody JiraConfigDto dto) {
        return ResponseEntity.ok(service.createConfig(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JiraConfigResponseDto> updateConfig(@PathVariable Long id,
            @Valid @RequestBody JiraConfigDto dto) {
        return ResponseEntity.ok(service.updateConfig(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        return null;
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<Void> triggerSync(@PathVariable Long id) {
        syncService.runManualSync(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync-all")
    @Operation(summary = "Sync all Jira configs", description = "Triggers a sync for all stored Jira configurations.")
    public ResponseEntity<Void> triggerAllSyncs() {
        syncService.runAllSyncs();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Boolean>> testConnection(@RequestBody JiraConfigDto dto) {
        boolean success = syncService.testConnection(dto);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @GetMapping("/audit")
    public ResponseEntity<Page<AuditLogDto>> getJiraSyncAudit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search) {
        return ResponseEntity.ok(auditService.getJiraIssueLogsByAction(JIRA_SYNC, PageRequest.of(page, size, Sort.by("timestamp").descending()), search));
    }
}
