package com.veritas.backend.workflow.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the results of BPMN workflow validation.
 * Errors are blocking – the workflow cannot be saved.
 * Warnings are advisory – the workflow can be saved but may behave unexpectedly.
 */
public class BpmnValidationResult {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void addError(String message) {
        errors.add(message);
    }

    public void addWarning(String message) {
        warnings.add(message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Returns a combined summary message for error responses.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        if (hasErrors()) {
            sb.append("Validation errors: ");
            sb.append(String.join("; ", errors));
        }
        if (hasWarnings()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("Warnings: ");
            sb.append(String.join("; ", warnings));
        }
        return sb.toString();
    }
}
