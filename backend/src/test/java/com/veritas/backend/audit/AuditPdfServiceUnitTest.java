package com.veritas.backend.audit;

import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.audit.service.impl.AuditPdfServiceImpl;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestItem;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.requisition.entity.RequestItemUnit;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.workflow.entity.WorkflowStep;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

// AI-GENERATED
@ExtendWith(MockitoExtension.class)
class AuditPdfServiceUnitTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private RequestRepository requestRepository;

    @InjectMocks
    private AuditPdfServiceImpl auditPdfService;

    private Request testRequest;
    private User testUser;

    @BeforeEach
    void setup() {
        testUser = User.builder()
                .name("Test User")
                .email("test@veritas.com")
                .build();

        testRequest = new Request();
        testRequest.setRequestName("Office Supplies Order");
        testRequest.setRequestKey("REQ-001");
        testRequest.setPriority(Priority.HIGH);
        testRequest.setState(RequestStatus.ACTIVE);
        testRequest.setUser(testUser);
        testRequest.setCreatedAt(LocalDateTime.of(2026, 6, 5, 10, 0));

        RequestItem item1 = new RequestItem();
        item1.setName("Printer Paper");
        item1.setDescription("A4 white, 80gsm");
        item1.setQuantity(10);
        item1.setUnit(RequestItemUnit.BOXES);
        item1.setRequest(testRequest);

        RequestItem item2 = new RequestItem();
        item2.setName("Ink Cartridges");
        item2.setDescription("Black, for HP LaserJet");
        item2.setQuantity(5);
        item2.setUnit(RequestItemUnit.PIECES);
        item2.setRequest(testRequest);

        testRequest.setItems(new ArrayList<>(List.of(item1, item2)));
    }

    @Test
    void generateAuditReport_WithAuditEntries_ReturnsValidPdf() throws IOException {
        Long requestId = 1L;
        List<AuditLog> auditLogs = createSampleAuditLogs();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));
        when(auditLogRepository.findAllByRequestIdOrderByTimestampDesc(requestId)).thenReturn(auditLogs);

        byte[] pdfBytes = auditPdfService.generateAuditReport(requestId);

        assertAll(
            () -> assertNotNull(pdfBytes),
            () -> assertTrue(pdfBytes.length > 0)
        );

        // Verify it's a valid PDF by loading it
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertTrue(doc.getNumberOfPages() >= 1);

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            assertAll(
                () -> assertTrue(text.contains("Audit Report")),
                () -> assertTrue(text.contains("Office Supplies Order")),
                () -> assertTrue(text.contains("REQ-001")),
                () -> assertTrue(text.contains("Printer Paper")),
                () -> assertTrue(text.contains("Submit")),
                () -> assertTrue(text.contains("Approve")),
                () -> assertTrue(text.contains("test@veritas.com"))
            );
        }
    }

    @Test
    void generateAuditReport_WithNoAuditEntries_ReturnsValidPdf() throws IOException {
        Long requestId = 2L;

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));
        when(auditLogRepository.findAllByRequestIdOrderByTimestampDesc(requestId)).thenReturn(List.of());

        byte[] pdfBytes = auditPdfService.generateAuditReport(requestId);

        assertAll(
            () -> assertNotNull(pdfBytes),
            () -> assertTrue(pdfBytes.length > 0)
        );

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertTrue(doc.getNumberOfPages() >= 1);

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            assertAll(
                () -> assertTrue(text.contains("Audit Report")),
                () -> assertTrue(text.contains("No audit entries found"))
            );
        }
    }

    @Test
    void generateAuditReport_WithNoLineItems_OmitsLineItemsSection() throws IOException {
        Long requestId = 3L;
        testRequest.setItems(new ArrayList<>());

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));
        when(auditLogRepository.findAllByRequestIdOrderByTimestampDesc(requestId)).thenReturn(List.of());

        byte[] pdfBytes = auditPdfService.generateAuditReport(requestId);

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            assertAll(
                () -> assertFalse(text.contains("Line Items")),
                () -> assertTrue(text.contains("Audit Report"))
            );
        }
    }

    @Test
    void generateAuditReport_RequestNotFound_ThrowsException() {
        Long requestId = 999L;
        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> auditPdfService.generateAuditReport(requestId));
        assertTrue(ex.getMessage().contains("Request not found"));
    }

    @Test
    void generateAuditReport_ManyEntries_CreatesMultiplePages() throws IOException {
        Long requestId = 4L;
        List<AuditLog> manyLogs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            AuditLog log = AuditLog.builder()
                    .actor(testUser)
                    .action("APPROVE")
                    .description("Approval step " + i)
                    .entryHash("hash-" + i)
                    .timestamp(LocalDateTime.now().plusMinutes(i))
                    .build();
            manyLogs.add(log);
        }

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));
        when(auditLogRepository.findAllByRequestIdOrderByTimestampDesc(requestId)).thenReturn(manyLogs);

        byte[] pdfBytes = auditPdfService.generateAuditReport(requestId);

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertTrue(doc.getNumberOfPages() > 1);
        }
    }

    @Test
    void generateAuditReport_WithUnicodeCharacters_DoesNotThrow() throws IOException {
        Long requestId = 5L;

        User unicodeUser = User.builder()
                .name("José Ångström — テスト")
                .email("unicode@example.com")
                .build();

        Request unicodeRequest = new Request();
        unicodeRequest.setRequestName("Order — “Special” — Café ☕");
        unicodeRequest.setRequestKey("REQ-ÜÑ1");
        unicodeRequest.setPriority(Priority.MEDIUM);
        unicodeRequest.setState(RequestStatus.ACTIVE);
        unicodeRequest.setUser(unicodeUser);
        unicodeRequest.setCreatedAt(LocalDateTime.now());

        RequestItem item = new RequestItem();
        item.setName("Café Crème — Größe");
        item.setDescription("Descr with emoji ✓ and dash — and accents: ö, é");
        item.setQuantity(1);
        item.setUnit(RequestItemUnit.PIECES);
        item.setRequest(unicodeRequest);
        unicodeRequest.setItems(new ArrayList<>(List.of(item)));

        List<AuditLog> unicodeLogs = new ArrayList<>();
        AuditLog log = AuditLog.builder()
                .actor(unicodeUser)
                .action("REQUISITION_EDITED")
                .description("Updated name to “Order — “Special” — Café ☕”")
                .entryHash("uhash")
                .timestamp(LocalDateTime.now())
                .build();
        unicodeLogs.add(log);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(unicodeRequest));
        when(auditLogRepository.findAllByRequestIdOrderByTimestampDesc(requestId)).thenReturn(unicodeLogs);

        byte[] pdfBytes = auditPdfService.generateAuditReport(requestId);

        assertAll(
            () -> assertNotNull(pdfBytes),
            () -> assertTrue(pdfBytes.length > 0)
        );

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertTrue(doc.getNumberOfPages() >= 1);
        }
    }

    // --- helpers ---

    private List<AuditLog> createSampleAuditLogs() {
        WorkflowStep submitStep = new WorkflowStep();
        submitStep.setName("Submit Procurement");

        WorkflowStep reviewStep = new WorkflowStep();
        reviewStep.setName("Team Leader Check");

        AuditLog log1 = AuditLog.builder()
                .actor(testUser)
                .action("SUBMIT")
                .description("Request submitted and entered workflow at: Submit Procurement")
                .newStep(submitStep)
                .entryHash("hash-1")
                .timestamp(LocalDateTime.of(2026, 6, 5, 10, 30))
                .build();

        AuditLog log2 = AuditLog.builder()
                .actor(testUser)
                .action("APPROVE")
                .description("Transitioned from Submit Procurement to Team Leader Check")
                .previousStep(submitStep)
                .newStep(reviewStep)
                .entryHash("hash-2")
                .timestamp(LocalDateTime.of(2026, 6, 5, 11, 0))
                .build();

        return List.of(log1, log2);
    }

    @Test
    void generateAuditReport_WithNullFields_HandlesNullsGracefully() throws IOException {
        Long requestId = 6L;
        Request nullRequest = new Request();
        
        RequestItem nullItem = new RequestItem();
        nullItem.setRequest(nullRequest);
        nullRequest.setItems(new ArrayList<>(List.of(nullItem)));

        AuditLog nullLog = new AuditLog();
        
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(nullRequest));
        when(auditLogRepository.findAllByRequestIdOrderByTimestampDesc(requestId)).thenReturn(List.of(nullLog));

        byte[] pdfBytes = auditPdfService.generateAuditReport(requestId);

        assertAll(
            () -> assertNotNull(pdfBytes),
            () -> assertTrue(pdfBytes.length > 0)
        );

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertTrue(doc.getNumberOfPages() >= 1);
        }
    }

    @Test
    void generateAuditReport_WithLongText_TriggersTextWrapping() throws IOException {
        Long requestId = 7L;
        Request longTextRequest = new Request();
        longTextRequest.setRequestName("A".repeat(100) + " " + "B".repeat(100));
        
        RequestItem longItem = new RequestItem();
        longItem.setName("ShortName " + "A".repeat(200));
        longItem.setDescription("ShortDesc " + "B".repeat(200) + " Word ".repeat(50));
        longItem.setRequest(longTextRequest);
        longTextRequest.setItems(new ArrayList<>(List.of(longItem)));

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(longTextRequest));
        when(auditLogRepository.findAllByRequestIdOrderByTimestampDesc(requestId)).thenReturn(List.of());

        byte[] pdfBytes = auditPdfService.generateAuditReport(requestId);

        assertAll(
            () -> assertNotNull(pdfBytes),
            () -> assertTrue(pdfBytes.length > 0)
        );

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertTrue(doc.getNumberOfPages() >= 1);
        }
    }

    @Test
    void generateAuditReport_WithNullItemsList_OmitsLineItemsSection() throws IOException {
        Long requestId = 8L;
        testRequest.setItems(null);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(testRequest));
        when(auditLogRepository.findAllByRequestIdOrderByTimestampDesc(requestId)).thenReturn(List.of());

        byte[] pdfBytes = auditPdfService.generateAuditReport(requestId);

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            assertAll(
                () -> assertFalse(text.contains("Line Items")),
                () -> assertTrue(text.contains("Audit Report"))
            );
        }
    }
}
