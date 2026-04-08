package com.veritas.backend.integrations;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/integrations/jira")
@Tag(name = "Integrations", description = "Endpoints for Jira Cloud")
public class JiraController {
    @Operation(summary = "Jira Webhook Receiver", description = "Listens for Jira 'Issue Created' events to auto-generate procurements.")
    @PostMapping("/webhook")
    public String handleJiraWebhook(@RequestBody Map<String, Object> payload) {
        return "Received Jira Webhook";
    }
}
