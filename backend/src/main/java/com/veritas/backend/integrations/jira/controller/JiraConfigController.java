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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/jira-configs", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@IsAdministrator
public class JiraConfigController {

    private final JiraConfigService service;
    private final JiraSyncService syncService;
    private final AuditServiceImpl auditService;

    @GetMapping
    public ResponseEntity<List<JiraConfigResponseDto>> getAllConfigs() {
        return ResponseEntity.ok(service.getAllConfigs());
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

    @PostMapping("/test")
    public ResponseEntity<Map<String, Boolean>> testConnection(@RequestBody JiraConfigDto dto) {
        boolean success = syncService.testConnection(dto);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AuditLogDto>> getJiraSyncAudit() {
        return ResponseEntity.ok(auditService.getJiraIssueLogsByAction("JIRA_SYNC"));
    }
}
