package com.veritas.backend.requisition.mapper;

import com.veritas.backend.requisition.dto.AttachmentDto;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.dto.RequisitionItemDto;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestQuote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Objects;

@Mapper(componentModel = "spring")
public interface RequisitionMapper {

    @Mapping(target = "id", source = "requestID")
    @Mapping(target = "projectName", source = "projectID.name")
    @Mapping(target = "projectKey", source = "projectID.projectKey")
    @Mapping(target = "workflowName", expression = "java(request.getWorkflowDefinitionID() != null ? request.getWorkflowDefinitionID().getName() : \"N/A\")")
    @Mapping(target = "teamName", source = "teamID.name")
    @Mapping(target = "requesterName", source = "userID.name")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "status", expression = "java(request.getCurrentStepID() != null ? request.getCurrentStepID().getName() : (request.getJiraIssueKey() != null ? \"Jira Synced\" : \"DRAFT\"))")
    @Mapping(target = "isClosed", expression = "java(request.getCurrentStepID() != null && com.veritas.backend.workflow.entity.WorkflowComponent.END_EVENT.equals(request.getCurrentStepID().getWorkflowComponent()))")
    @Mapping(target = "responsibleRole", expression = "java(request.getCurrentStepID() != null && request.getCurrentStepID().getRole() != null ? request.getCurrentStepID().getRole().name() : (request.getJiraIssueKey() != null && request.getCurrentStepID() == null ? \"REQUESTER\" : null))")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "jiraIssueKey", source = "jiraIssueKey")
    @Mapping(target = "jiraIssueUrl", source = "jiraIssueUrl")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "attachments", source = "attachments")
    @Mapping(target = "vendorId", expression = "java(resolveSelectedVendorId(request))")
    @Mapping(target = "vendorName", expression = "java(resolveSelectedVendorName(request))")
    @Mapping(target = "isEvaluated", expression = "java(request.getVendorEvaluations() != null && !request.getVendorEvaluations().isEmpty())")
    RequisitionDto toDto(Request request);

    RequisitionItemDto toItemDto(com.veritas.backend.requisition.entity.RequestItem item);
    
    AttachmentDto toAttachmentDto(com.veritas.backend.requisition.entity.Attachment attachment);

    default Long resolveSelectedVendorId(Request request) {
        if (request.getQuotes() == null) return null;
        return request.getQuotes().stream()
                .filter(rq -> Boolean.TRUE.equals(rq.getIsSelected()))
                .map(rq -> rq.getQuote().getVendorID() != null ? rq.getQuote().getVendorID().getId() : null)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    default String resolveSelectedVendorName(Request request) {
        if (request.getQuotes() == null) return null;
        return request.getQuotes().stream()
                .filter(rq -> Boolean.TRUE.equals(rq.getIsSelected()))
                .map(rq -> rq.getQuote().getVendorID() != null ? rq.getQuote().getVendorID().getVendorName() : null)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
