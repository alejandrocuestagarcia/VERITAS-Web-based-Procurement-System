package com.veritas.backend.requisition.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.veritas.backend.project.entity.Project;
import com.veritas.backend.requisition.dto.AttachmentDto;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.dto.RequisitionItemDto;
import com.veritas.backend.requisition.entity.Attachment;
import com.veritas.backend.requisition.entity.Invoice;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestItem;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.requisition.entity.RequestItemUnit;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.requisition.mapper.RequisitionMapper;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class RequisitionMapperUnitTest {

    private RequisitionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(RequisitionMapper.class);
    }

    @Test
    void toDto_NullRequest_ReturnsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toItemDto_NullItem_ReturnsNull() {
        assertNull(mapper.toItemDto(null));
    }

    @Test
    void toAttachmentDto_NullAttachment_ReturnsNull() {
        assertNull(mapper.toAttachmentDto(null));
    }

    @Test
    void toDto_WithCurrentStep_MapsStepNameAsCurrentStep() {
        WorkflowStep step = new WorkflowStep();
        step.setName("Manager Approval");

        Request request = buildRequest();
        request.setCurrentStep(step);

        RequisitionDto dto = mapper.toDto(request);
        assertEquals("Manager Approval", dto.status());
    }

    @Test
    void toDto_ResponsibleRoleEdgeCases() {
        WorkflowStep step = new WorkflowStep();
        step.setRole(UserRole.FINANCE_OFFICER);

        Request request = buildRequest();
        request.setCurrentStep(step);

        assertAll(
            () -> {
                RequisitionDto dto = mapper.toDto(request);
                assertEquals("FINANCE_OFFICER", dto.responsibleRole());
            },
            () -> {
                step.setRole(null);
                RequisitionDto dto = mapper.toDto(request);
                assertNull(dto.responsibleRole());
            }
        );
    }

    @Test
    void toDto_IsClosedEdgeCases() {
        Request request = buildRequest();
        assertAll(
            () -> {
                request.setState(RequestStatus.FINISHED);
                RequisitionDto dto = mapper.toDto(request);
                assertTrue(dto.isClosed());
            },
            () -> {
                request.setState(RequestStatus.ACTIVE);
                RequisitionDto dto = mapper.toDto(request);
                assertFalse(dto.isClosed());
            }
        );
    }

    @Test
    void toDto_InvoiceEdgeCases() {
        Request request = buildRequest();
        assertAll(
            () -> {
                request.setInvoice(null);
                RequisitionDto dto = mapper.toDto(request);
                assertFalse(dto.isPaid());
                assertNull(dto.paidAmountEur());
            },
            () -> {
                Invoice invoice = new Invoice();
                invoice.setIsPaid(true);
                invoice.setPaidAmountEur(BigDecimal.TEN);
                request.setInvoice(invoice);
                RequisitionDto dto = mapper.toDto(request);
                assertTrue(dto.isPaid());
                assertEquals(BigDecimal.TEN, dto.paidAmountEur());
            },
            () -> {
                Invoice invoice = new Invoice();
                invoice.setIsPaid(false);
                invoice.setPaidAmountEur(BigDecimal.ZERO);
                request.setInvoice(invoice);
                RequisitionDto dto = mapper.toDto(request);
                assertFalse(dto.isPaid());
                assertEquals(BigDecimal.ZERO, dto.paidAmountEur());
            }
        );
    }

    @Test
    void toDto_WorkflowStepDescription() {
        WorkflowStep step = new WorkflowStep();
        step.setDescription("Step Desc");

        Request request = buildRequest();
        request.setCurrentStep(step);

        assertAll(
            () -> {
                RequisitionDto dto = mapper.toDto(request);
                assertEquals("Step Desc", dto.workflowStepDescription());
            },
            () -> {
                step.setDescription(null);
                RequisitionDto dto = mapper.toDto(request);
                assertNull(dto.workflowStepDescription());
            }
        );
    }

    @Test
    void toDto_resolveSelectedVendorEdgeCases() {
        Request request = buildRequest();
        assertAll(
            () -> {
                request.setQuotes(Collections.emptyList());
                assertNull(mapper.resolveSelectedVendorId(request));
                assertNull(mapper.resolveSelectedVendorName(request));
            },
            () -> {
                Quote q1 = new Quote();
                q1.setSelected(false);
                request.setQuotes(List.of(q1));
                assertNull(mapper.resolveSelectedVendorId(request));
                assertNull(mapper.resolveSelectedVendorName(request));
            },
            () -> {
                Quote q1 = new Quote();
                q1.setSelected(true);
                q1.setVendorID(null);
                request.setQuotes(List.of(q1));
                assertNull(mapper.resolveSelectedVendorId(request));
                assertNull(mapper.resolveSelectedVendorName(request));
            },
            () -> {
                Vendor vendor = new Vendor();
                vendor.setId(123L);
                vendor.setVendorName("Vendor Corp");
                Quote q1 = new Quote();
                q1.setSelected(true);
                q1.setVendorID(vendor);
                request.setQuotes(List.of(q1));
                assertEquals(123L, mapper.resolveSelectedVendorId(request));
                assertEquals("Vendor Corp", mapper.resolveSelectedVendorName(request));
            }
        );
    }

    @Test
    void toDto_NullAssociationFields_MapsCorrectly() {
        Request request = new Request();
        request.setCurrentStep(new WorkflowStep());
        request.setProject(null);
        request.setTeam(null);
        request.setUser(null);
        request.setAssignee(null);
        request.setWorkflowDefinition(null);

        RequisitionDto dto = mapper.toDto(request);
        assertAll(
            () -> assertNull(dto.projectName()),
            () -> assertNull(dto.projectKey()),
            () -> assertNull(dto.workflowName()),
            () -> assertNull(dto.teamName()),
            () -> assertNull(dto.requesterName()),
            () -> assertNull(dto.requesterId()),
            () -> assertNull(dto.requesterTeamId()),
            () -> assertNull(dto.assigneeId()),
            () -> assertNull(dto.assigneeName()),
            () -> assertNull(dto.assigneeEmail()),
            () -> assertNull(dto.workflowDefinitionId())
        );
    }

    @Test
    void toDto_NullLists_MapsNullLists() {
        Request request = new Request();
        request.setCurrentStep(new WorkflowStep());
        request.setItems(null);
        request.setAttachments(null);

        RequisitionDto dto = mapper.toDto(request);
        assertAll(
            () -> assertNull(dto.items()),
            () -> assertNull(dto.attachments())
        );
    }

    @Test
    void toDto_WithAssociations_MapsCorrectly() {
        Project project = new Project();
        project.setName("Project A");
        project.setProjectKey("PROJ-A");

        Team userTeam = new Team();
        userTeam.setTeamId(2L);

        User user = new User();
        user.setId(10L);
        user.setName("John Requester");
        user.setTeam(userTeam);

        User assignee = new User();
        assignee.setId(11L);
        assignee.setName("Manager B");
        assignee.setEmail("managerb@veritas.com");

        Team requestTeam = new Team();
        requestTeam.setName("Team Engineering");

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(5L);
        workflow.setName("Standard Flow");

        Request request = new Request();
        request.setProject(project);
        request.setUser(user);
        request.setAssignee(assignee);
        request.setTeam(requestTeam);
        request.setWorkflowDefinition(workflow);

        WorkflowStep step = new WorkflowStep();
        step.setName("Start");
        step.setRole(UserRole.REQUESTER);
        request.setCurrentStep(step);

        RequisitionDto dto = mapper.toDto(request);
        assertAll(
            () -> assertEquals("Project A", dto.projectName()),
            () -> assertEquals("PROJ-A", dto.projectKey()),
            () -> assertEquals("Standard Flow", dto.workflowName()),
            () -> assertEquals("Team Engineering", dto.teamName()),
            () -> assertEquals("John Requester", dto.requesterName()),
            () -> assertEquals(10L, dto.requesterId()),
            () -> assertEquals(2L, dto.requesterTeamId()),
            () -> assertEquals(11L, dto.assigneeId()),
            () -> assertEquals("Manager B", dto.assigneeName()),
            () -> assertEquals("managerb@veritas.com", dto.assigneeEmail()),
            () -> assertEquals(5L, dto.workflowDefinitionId())
        );
    }

    @Test
    void toItemDto_MapsCorrectly() {
        RequestItem item = new RequestItem();
        item.setId(1L);
        item.setName("Item A");
        item.setQuantity(5);
        item.setUnit(RequestItemUnit.PIECES);
        item.setDescription("Item Desc");

        RequisitionItemDto dto = mapper.toItemDto(item);
        assertAll(
            () -> assertEquals(1L, dto.id()),
            () -> assertEquals("Item A", dto.name()),
            () -> assertEquals(5, dto.quantity()),
            () -> assertEquals(RequestItemUnit.PIECES, dto.unit()),
            () -> assertEquals("Item Desc", dto.description())
        );
    }

    @Test
    void toAttachmentDto_MapsCorrectly() {
        Attachment attachment = new Attachment();
        attachment.setAttachmentId(2L);
        attachment.setFileName("file.pdf");
        attachment.setStoragePath("/tmp/file.pdf");
        attachment.setFileType("application/pdf");
        attachment.setFileSize(100L);

        AttachmentDto dto = mapper.toAttachmentDto(attachment);
        assertAll(
            () -> assertEquals(2L, dto.attachmentId()),
            () -> assertEquals("file.pdf", dto.fileName()),
            () -> assertEquals("/tmp/file.pdf", dto.storagePath()),
            () -> assertEquals("application/pdf", dto.fileType()),
            () -> assertEquals(100L, dto.fileSize())
        );
    }

    @Test
    void toDto_WithNestedNullProperties_MapsCorrectly() {
        Request request = new Request();
        
        Project project = new Project();
        project.setName(null);
        project.setProjectKey(null);
        request.setProject(project);

        Team team = new Team();
        team.setName(null);
        request.setTeam(team);

        User user = new User();
        user.setName(null);
        user.setId(null);
        
        Team userTeam = new Team();
        userTeam.setTeamId(null);
        user.setTeam(userTeam);
        request.setUser(user);

        User assignee = new User();
        assignee.setId(null);
        assignee.setName(null);
        assignee.setEmail(null);
        request.setAssignee(assignee);

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(null);
        request.setWorkflowDefinition(workflow);

        WorkflowStep step = new WorkflowStep();
        step.setName(null);
        step.setRole(null);
        request.setCurrentStep(step);

        RequisitionDto dto = mapper.toDto(request);
        assertAll(
            () -> assertNull(dto.projectName()),
            () -> assertNull(dto.projectKey()),
            () -> assertNull(dto.teamName()),
            () -> assertNull(dto.requesterName()),
            () -> assertNull(dto.requesterId()),
            () -> assertNull(dto.requesterTeamId()),
            () -> assertNull(dto.assigneeId()),
            () -> assertNull(dto.assigneeName()),
            () -> assertNull(dto.assigneeEmail()),
            () -> assertNull(dto.workflowDefinitionId())
        );
    }

    private Request buildRequest() {
        Project project = new Project();
        project.setName("Test Project");

        Team team = new Team();
        team.setName("Engineering");

        User user = new User();
        user.setName("Max Mustermann");

        WorkflowStep step = new WorkflowStep();
        step.setName("Start");

        Request request = new Request();
        request.setRequestID(42L);
        request.setRequestName("Test Request");
        request.setRequestKey("PRJ-1");
        request.setPriority(Priority.HIGH);
        request.setProject(project);
        request.setTeam(team);
        request.setUser(user);
        request.setCurrentStep(step);
        return request;
    }
}
