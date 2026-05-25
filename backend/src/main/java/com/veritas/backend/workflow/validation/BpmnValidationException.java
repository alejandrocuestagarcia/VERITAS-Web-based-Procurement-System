package com.veritas.backend.workflow.validation;

import java.util.List;

/**
 * Thrown when BPMN XML fails structural or semantic validation.
 * Carries the full list of errors so they can all be returned to the client.
 */
public class BpmnValidationException extends RuntimeException {

    private final BpmnValidationResult validationResult;

    public BpmnValidationException(BpmnValidationResult validationResult) {
        super(validationResult.getSummary());
        this.validationResult = validationResult;
    }

    public BpmnValidationResult getValidationResult() {
        return validationResult;
    }

    public List<String> getErrors() {
        return validationResult.getErrors();
    }

    public List<String> getWarnings() {
        return validationResult.getWarnings();
    }
}
