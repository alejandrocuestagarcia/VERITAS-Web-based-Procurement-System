package com.veritas.backend.integrations.jira.mapper;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veritas.backend.integrations.jira.dto.JiraFieldsRecord;
import com.veritas.backend.integrations.jira.dto.JiraIssueRecord;
import com.veritas.backend.integrations.jira.dto.JiraPriorityRecord;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.requisition.entity.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

// AI-GENERATED

public class JiraIssueMapperUnitTest {

    private JiraIssueMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(JiraIssueMapper.class);
        objectMapper = new ObjectMapper();
    }

    @Test
    void toRequest_ShouldMapAllFields() {
        ObjectNode adfDescription = objectMapper.createObjectNode();
        adfDescription.put("version", 1);
        adfDescription.put("type", "doc");

        JiraFieldsRecord fields = new JiraFieldsRecord(
            "Summary Test",
            adfDescription,
            new JiraPriorityRecord("High"),
            "2024-04-26T14:30:00.000+0000",
            "2024-04-26T15:30:00.000+0000",
            null,
            null
        );

        JiraIssueRecord issue =
            new JiraIssueRecord("10001", "TEST-1", "https://api.test/1", fields);

        Request request = mapper.toRequest(issue);

        assertEquals("TEST-1", request.getJiraIssueKey());
        assertEquals("https://api.test/1", request.getJiraIssueUrl());
        assertEquals("Summary Test", request.getRequestName());
        assertEquals(Priority.HIGH, request.getPriority());
        assertEquals(adfDescription.toString(), request.getDescription());
        assertEquals("PENDING_SYNC", request.getJiraStatus());
    }

    @Test
    void mapPriority_ShouldHandleDifferentInputs() {
        assertEquals(Priority.CRITICAL, mapper.mapPriority("Critical"));
        assertEquals(Priority.CRITICAL, mapper.mapPriority("Highest"));
        assertEquals(Priority.HIGH, mapper.mapPriority("High"));
        assertEquals(Priority.MEDIUM, mapper.mapPriority("Medium"));
        assertEquals(Priority.MEDIUM, mapper.mapPriority("Major"));
        assertEquals(Priority.LOW, mapper.mapPriority("Low"));
        assertEquals(Priority.LOW, mapper.mapPriority("Lowest"));
        assertEquals(Priority.MEDIUM, mapper.mapPriority(null));
        assertEquals(Priority.MEDIUM, mapper.mapPriority("Unknown"));
    }
}
