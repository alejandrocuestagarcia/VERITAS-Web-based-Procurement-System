package com.veritas.backend.workflow.entity;

public enum AttachmentType {
    PDF("PDF Document (PDF)"),
    CSV("CSV Spreadsheet (CSV)"),
    IMAGE("Image File (PNG, JPG, JPEG, GIF)"),
    EXCEL("Excel Spreadsheet (XLSX, XLS)"),
    WORD("Word Document (DOCX, DOC)"),
    POWERPOINT("PowerPoint Presentation (PPTX, PPT)"),
    ZIP("ZIP Archive (ZIP)"),
    EMAIL("Email Message (EML, MSG)");

    private final String label;

    AttachmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
