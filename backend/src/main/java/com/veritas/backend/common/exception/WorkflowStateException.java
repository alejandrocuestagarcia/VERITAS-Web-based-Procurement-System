package com.veritas.backend.common.exception;

/**
 * Thrown when an action is performed on a workflow request
 * that is invalid for its current state (e.g., approving a FINISHED request).
 */
public class WorkflowStateException extends RuntimeException {

    public WorkflowStateException(String message) {
        super(message);
    }

    public WorkflowStateException(String message, Throwable cause) {
        super(message, cause);
    }
}