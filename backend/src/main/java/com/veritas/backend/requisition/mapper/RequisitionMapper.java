package com.veritas.backend.requisition.mapper;

import com.veritas.backend.requisition.dto.AttachmentDto;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.dto.RequisitionItemDto;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.vendor.entity.Quote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Objects;

@Mapper(componentModel = "spring")
public interface RequisitionMapper {

    @Mapping(target = "id", source = "requestID")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "projectKey", source = "project.projectKey")
    @Mapping(target = "workflowName", source = "workflowDefinition.name")
    @Mapping(target = "teamName", source = "team.name")
    @Mapping(target = "requesterName", source = "user.name")
    @Mapping(target = "requesterId", source = "user.id")
    @Mapping(target = "requesterTeamId", source = "user.team.teamId")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "status", source = "currentStep.name")
    @Mapping(target = "isClosed", expression = "java(request.getState() == com.veritas.backend.requisition.entity.RequestStatus.FINISHED)")
    @Mapping(target = "responsibleRole", expression = "java(request.getCurrentStep().getRole() != null ? request.getCurrentStep().getRole().name() : null)")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "jiraIssueKey", source = "jiraIssueKey")
    @Mapping(target = "jiraIssueUrl", source = "jiraIssueUrl")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "attachments", source = "attachments")
    @Mapping(target = "vendorId", expression = "java(resolveSelectedVendorId(request))")
    @Mapping(target = "vendorName", expression = "java(resolveSelectedVendorName(request))")
    @Mapping(target = "isEvaluated", expression = "java(request.getVendorEvaluation() != null)")
    @Mapping(target = "assigneeId", source = "assignee.id")
    @Mapping(target = "assigneeName", source = "assignee.name")
    @Mapping(target = "assigneeEmail", source = "assignee.email")
    @Mapping(target = "workflowDefinitionId", source = "workflowDefinition.id")
    @Mapping(target = "isPaid", expression = "java(request.getInvoice() != null && request.getInvoice().getIsPaid())")
    @Mapping(target = "paidAmountEur", expression = "java(request.getInvoice() != null ? request.getInvoice().getPaidAmountEur() : null)")
    @Mapping(target = "workflowStepDescription", source = "currentStep.description")
    @Mapping(target = "revisionRequired", source = "revisionRequired")
    RequisitionDto toDto(Request request);

    RequisitionItemDto toItemDto(com.veritas.backend.requisition.entity.RequestItem item);
    
    @Mapping(target = "invoiceId", source = "invoice.invoiceId")
    AttachmentDto toAttachmentDto(com.veritas.backend.requisition.entity.Attachment attachment);

    default Long resolveSelectedVendorId(Request request) {
        Quote selected = request.getSelectedQuote();
        return (selected != null && selected.getVendorID() != null) ? selected.getVendorID().getId() : null;
    }

    default String resolveSelectedVendorName(Request request) {
        Quote selected = request.getSelectedQuote();
        return (selected != null && selected.getVendorID() != null) ? selected.getVendorID().getVendorName() : null;
    }
}
