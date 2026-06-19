package com.veritas.backend.audit.service.impl;

import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.audit.service.AuditPdfService;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestItem;
import com.veritas.backend.requisition.repository.RequestRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditPdfServiceImpl implements AuditPdfService {

    private final AuditLogRepository auditLogRepository;
    private final RequestRepository requestRepository;

    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float ROW_HEIGHT = 18;
    private static final float HEADER_ROW_HEIGHT = 22;
    private static final float SECTION_GAP = 28;
    private static final float FONT_SIZE_TITLE = 18;
    private static final float FONT_SIZE_SECTION = 12;
    private static final float FONT_SIZE_BODY = 9;
    private static final float FONT_SIZE_SMALL = 7.5f;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a");
    private static final DateTimeFormatter DATE_ONLY_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    @Override
    public byte[] generateAuditReport(Long requestId) {
        Request request = requestRepository.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("Request not found with ID: " + requestId));

        List<AuditLog> auditLogs = auditLogRepository.findAllByRequestIdOrderByTimestampDesc(requestId);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PdfContext ctx = new PdfContext(document);
            ctx.newPage();

            drawHeader(ctx, request);
            drawMetadataSection(ctx, request);
            drawLineItemsSection(ctx, request);
            drawAuditLogSection(ctx, auditLogs);
            drawFooter(ctx);

            ctx.closeStream();
            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate audit PDF for request {}", requestId, e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    // --- drawing methods ---
    // They have been AI-REFACTORED

    private void drawHeader(PdfContext ctx, Request request) throws IOException {
        // Title
        ctx.stream.setFont(FONT_BOLD, FONT_SIZE_TITLE);
        ctx.stream.setNonStrokingColor(0.07f, 0.07f, 0.15f);
        ctx.stream.beginText();
        ctx.stream.newLineAtOffset(MARGIN, ctx.y);
        ctx.stream.showText("Audit Report");
        ctx.stream.endText();
        ctx.y -= 22;

        // Request name
        ctx.stream.setFont(FONT_REGULAR, FONT_SIZE_SECTION);
        ctx.stream.setNonStrokingColor(0.35f, 0.35f, 0.45f);
        ctx.stream.beginText();
        ctx.stream.newLineAtOffset(MARGIN, ctx.y);
        ctx.stream.showText(sanitize(request.getRequestName()));
        ctx.stream.endText();
        ctx.y -= 16;

        // Generation timestamp
        ctx.stream.setFont(FONT_REGULAR, FONT_SIZE_SMALL);
        ctx.stream.setNonStrokingColor(0.55f, 0.55f, 0.6f);
        ctx.stream.beginText();
        ctx.stream.newLineAtOffset(MARGIN, ctx.y);
        ctx.stream.showText("Generated on " + LocalDateTime.now().format(DATE_FORMAT));
        ctx.stream.endText();
        ctx.y -= 10;

        // Separator line
        drawHorizontalLine(ctx, 0.85f, 0.85f, 0.9f);
        ctx.y -= SECTION_GAP;
    }

    private void drawMetadataSection(PdfContext ctx, Request request) throws IOException {
        ctx.ensureSpace(120);

        drawSectionTitle(ctx, "Request Details");

        String[][] metadata = {
                {"Request Key", defaultStr(request.getRequestKey())},
                {"Requester", request.getUser() != null ? defaultStr(request.getUser().getEmail()) : "N/A"},
                {"Team", request.getTeam() != null ? defaultStr(request.getTeam().getName()) : "N/A"},
                {"Project", request.getProject() != null ? defaultStr(request.getProject().getName()) : "N/A"},
                {"Priority", request.getPriority() != null ? request.getPriority().name() : "N/A"},
                {"State", request.getState() != null ? request.getState().name() : "N/A"},
                {"Created", request.getCreatedAt() != null ? request.getCreatedAt().format(DATE_ONLY_FORMAT) : "N/A"},
        };

        for (String[] row : metadata) {
            ctx.ensureSpace(ROW_HEIGHT);
            ctx.stream.setFont(FONT_BOLD, FONT_SIZE_BODY);
            ctx.stream.setNonStrokingColor(0.35f, 0.35f, 0.45f);
            ctx.stream.beginText();
            ctx.stream.newLineAtOffset(MARGIN + 4, ctx.y);
            ctx.stream.showText(row[0]);
            ctx.stream.endText();

            ctx.stream.setFont(FONT_REGULAR, FONT_SIZE_BODY);
            ctx.stream.setNonStrokingColor(0.1f, 0.1f, 0.15f);
            ctx.stream.beginText();
            ctx.stream.newLineAtOffset(MARGIN + 130, ctx.y);
            ctx.stream.showText(sanitize(row[1]));
            ctx.stream.endText();

            ctx.y -= ROW_HEIGHT;
        }

        ctx.y -= SECTION_GAP;
    }

    private void drawLineItemsSection(PdfContext ctx, Request request) throws IOException {
        List<RequestItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            return;
        }

        ctx.ensureSpace(HEADER_ROW_HEIGHT + ROW_HEIGHT + SECTION_GAP);

        drawSectionTitle(ctx, "Line Items");

        // Column widths: Name(40%), Description(35%), Qty(10%), Unit(15%)
        float[] colWidths = {CONTENT_WIDTH * 0.30f, CONTENT_WIDTH * 0.40f, CONTENT_WIDTH * 0.12f, CONTENT_WIDTH * 0.18f};
        String[] headers = {"Name", "Description", "Qty", "Unit"};

        drawTableHeader(ctx, headers, colWidths);

        for (int i = 0; i < items.size(); i++) {
            RequestItem item = items.get(i);

            String[] rowData = {
                    defaultStr(item.getName()),
                    defaultStr(item.getDescription()),
                    String.valueOf(item.getQuantity()),
                    item.getUnit() != null ? item.getUnit().toString() : "N/A"
            };

            drawTableRow(ctx, rowData, colWidths, i % 2 == 0);
        }

        ctx.y -= SECTION_GAP;
    }

    private void drawAuditLogSection(PdfContext ctx, List<AuditLog> auditLogs) throws IOException {
        ctx.ensureSpace(HEADER_ROW_HEIGHT + ROW_HEIGHT + SECTION_GAP);

        drawSectionTitle(ctx, "Audit Log (" + auditLogs.size() + " Entries)");

        if (auditLogs.isEmpty()) {
            ctx.stream.setFont(FONT_REGULAR, FONT_SIZE_BODY);
            ctx.stream.setNonStrokingColor(0.5f, 0.5f, 0.55f);
            ctx.stream.beginText();
            ctx.stream.newLineAtOffset(MARGIN + 4, ctx.y);
            ctx.stream.showText("No audit entries found for this request.");
            ctx.stream.endText();
            ctx.y -= ROW_HEIGHT;
            return;
        }

        // Column widths: Timestamp(20%), Action(10%), User(20%), From(12%), To(12%), Description(26%)
        float[] colWidths = {
                CONTENT_WIDTH * 0.20f,
                CONTENT_WIDTH * 0.10f,
                CONTENT_WIDTH * 0.20f,
                CONTENT_WIDTH * 0.12f,
                CONTENT_WIDTH * 0.12f,
                CONTENT_WIDTH * 0.26f
        };
        String[] headers = {"Timestamp", "Action", "User", "From", "To", "Description"};

        drawTableHeader(ctx, headers, colWidths);

        for (int i = 0; i < auditLogs.size(); i++) {
            AuditLog logEntry = auditLogs.get(i);

            String[] rowData = {
                    logEntry.getTimestamp() != null ? logEntry.getTimestamp().format(DATE_FORMAT) : "",
                    formatAction(logEntry.getAction()),
                    logEntry.getActor() != null ? logEntry.getActor().getEmail() : "System",
                    logEntry.getPreviousStep() != null ? logEntry.getPreviousStep().getName() : "",
                    logEntry.getNewStep() != null ? logEntry.getNewStep().getName() : "",
                    defaultStr(logEntry.getDescription())
            };

            drawTableRow(ctx, rowData, colWidths, i % 2 == 0);
        }
    }

    private void drawFooter(PdfContext ctx) throws IOException {
        // Draw footer on every page with correct current/total page numbering.
        int totalPages = ctx.document.getNumberOfPages();
        // Position the footer a small distance above the bottom margin.
        float lineY = MARGIN + 10;
        float textY = lineY - 14;

        for (int i = 0; i < totalPages; i++) {
            PDPage page = ctx.document.getPage(i);
            try (PDPageContentStream footerStream = new PDPageContentStream(ctx.document, page, AppendMode.APPEND, true, true)) {
                // Separator line
                footerStream.setStrokingColor(0.85f, 0.85f, 0.9f);
                footerStream.setLineWidth(0.5f);
                footerStream.moveTo(MARGIN, lineY);
                footerStream.lineTo(PAGE_WIDTH - MARGIN, lineY);
                footerStream.stroke();

                // Left text
                footerStream.setFont(FONT_REGULAR, FONT_SIZE_SMALL);
                footerStream.setNonStrokingColor(0.6f, 0.6f, 0.65f);
                footerStream.beginText();
                footerStream.newLineAtOffset(MARGIN, textY);
                footerStream.showText("Veritas Procurement System — Confidential Audit Report");
                footerStream.endText();

                // Right-aligned page count
                String pageText = "Page " + (i + 1) + " of " + totalPages;
                float textWidth = FONT_REGULAR.getStringWidth(pageText) / 1000 * FONT_SIZE_SMALL;
                footerStream.beginText();
                footerStream.newLineAtOffset(PAGE_WIDTH - MARGIN - textWidth, textY);
                footerStream.showText(pageText);
                footerStream.endText();
            }
        }
    }

    // --- table drawing helpers ---
    // They have been AI-REFACTORED

    private void drawSectionTitle(PdfContext ctx, String title) throws IOException {
        ctx.stream.setFont(FONT_BOLD, FONT_SIZE_SECTION);
        ctx.stream.setNonStrokingColor(0.07f, 0.07f, 0.15f);
        ctx.stream.beginText();
        ctx.stream.newLineAtOffset(MARGIN, ctx.y);
        ctx.stream.showText(title);
        ctx.stream.endText();
        ctx.y -= 20;
    }

    private void drawTableHeader(PdfContext ctx, String[] headers, float[] colWidths) throws IOException {
        ctx.y -= 12; // Extra spacing between section title and table

        // Header background
        ctx.stream.setNonStrokingColor(0.94f, 0.95f, 0.97f);
        ctx.stream.addRect(MARGIN, ctx.y - 4, CONTENT_WIDTH, HEADER_ROW_HEIGHT);
        ctx.stream.fill();

        // Header text
        ctx.stream.setFont(FONT_BOLD, FONT_SIZE_SMALL);
        ctx.stream.setNonStrokingColor(0.3f, 0.3f, 0.4f);
        float xPos = MARGIN + 4;
        for (int i = 0; i < headers.length; i++) {
            ctx.stream.beginText();
            ctx.stream.newLineAtOffset(xPos, ctx.y + 4);
            ctx.stream.showText(headers[i].toUpperCase());
            ctx.stream.endText();
            xPos += colWidths[i];
        }

        ctx.y -= HEADER_ROW_HEIGHT;
    }

    private void drawTableRow(PdfContext ctx, String[] values, float[] colWidths, boolean isEven) throws IOException {
        java.util.List<java.util.List<String>> columnLines = new java.util.ArrayList<>();
        int maxLines = 1;
        for (int i = 0; i < values.length; i++) {
            float availableWidth = colWidths[i] - 8; 
            java.util.List<String> lines = splitText(sanitize(values[i]), availableWidth, FONT_REGULAR, FONT_SIZE_SMALL);
            columnLines.add(lines);
            if (lines.size() > maxLines) {
                maxLines = lines.size();
            }
        }
        
        float lineHeight = 10;
        float rowHeight = (maxLines * lineHeight) + 8;
        
        ctx.ensureSpace(rowHeight);
        
        if (isEven) {
            ctx.stream.setNonStrokingColor(0.96f, 0.97f, 0.98f);
            ctx.stream.addRect(MARGIN, ctx.y + 14 - rowHeight, CONTENT_WIDTH, rowHeight);
            ctx.stream.fill();
        }
        
        ctx.stream.setFont(FONT_REGULAR, FONT_SIZE_SMALL);
        ctx.stream.setNonStrokingColor(0.15f, 0.15f, 0.2f);
        
        float xPos = MARGIN + 4;
        for (int i = 0; i < values.length; i++) {
            java.util.List<String> lines = columnLines.get(i);
            float yPos = ctx.y + 2; 
            for (String line : lines) {
                ctx.stream.beginText();
                ctx.stream.newLineAtOffset(xPos, yPos);
                ctx.stream.showText(line);
                ctx.stream.endText();
                yPos -= lineHeight;
            }
            xPos += colWidths[i];
        }
        ctx.y -= rowHeight;
    }

    private java.util.List<String> splitText(String text, float width, PDType1Font font, float fontSize) throws IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            float wordWidth = font.getStringWidth(word) / 1000 * fontSize;
            if (wordWidth > width) {
                // Word is too long, we need to character-wrap it
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
                
                StringBuilder chunk = new StringBuilder();
                for (int i = 0; i < word.length(); i++) {
                    char c = word.charAt(i);
                    String testChunk = chunk.toString() + c;
                    float testChunkWidth = font.getStringWidth(testChunk) / 1000 * fontSize;
                    if (testChunkWidth > width && chunk.length() > 0) {
                        lines.add(chunk.toString());
                        chunk = new StringBuilder().append(c);
                    } else {
                        chunk.append(c);
                    }
                }
                if (chunk.length() > 0) {
                    currentLine = chunk;
                }
            } else {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                float testWidth = font.getStringWidth(testLine) / 1000 * fontSize;
                if (testWidth > width && currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testLine);
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private void drawHorizontalLine(PdfContext ctx, float r, float g, float b) throws IOException {
        ctx.stream.setStrokingColor(r, g, b);
        ctx.stream.setLineWidth(0.5f);
        ctx.stream.moveTo(MARGIN, ctx.y);
        ctx.stream.lineTo(PAGE_WIDTH - MARGIN, ctx.y);
        ctx.stream.stroke();
    }

    // --- utility methods ---

    private String formatAction(String action) {
        if (action == null) return "";
        String[] words = action.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    // AI-GENERATED
    private static String sanitize(String text) {
        if (text == null) return "";
        // Normalize whitespace/newlines first
        String s = text.replace("\t", "    ")
            .replace("\r\n", " ")
            .replace("\n", " ")
            .replace("\r", " ");

        // Common Unicode->ASCII replacements for characters often present in text
        s = s.replace("–", "-")
            .replace("—", "-")
            .replace("“", "\"")
            .replace("”", "\"")
            .replace("‘", "'")
            .replace("’", "'")
            .replace("…", "...")
            .replace("•", "-")
            .replace("©", "(c)")
            .replace("®", "(r)")
            .replace("™", "(tm)");

        // PDFBox Standard 14 fonts cover a limited glyph set; replace any remaining
        // characters outside Latin-1 (ISO-8859-1) with a safe placeholder to avoid
        // runtime encoding errors when calling showText(). This preserves readability
        // while preventing exceptions in PDF generation.
        s = s.replaceAll("[^\\u0000-\\u00FF]", "?");

        return s;
    }

    private static String defaultStr(String value) {
        return value != null ? value : "N/A";
    }

    /**
     * Mutable context carried across drawing methods, managing pagination.
     */
    private static class PdfContext {
        final PDDocument document;
        PDPageContentStream stream;
        float y;

        PdfContext(PDDocument document) {
            this.document = document;
        }

        void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_HEIGHT - MARGIN;
        }

        void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight < MARGIN) {
                newPage();
            }
        }

        void closeStream() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
