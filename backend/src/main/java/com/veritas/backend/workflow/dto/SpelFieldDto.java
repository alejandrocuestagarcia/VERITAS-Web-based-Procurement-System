package com.veritas.backend.workflow.dto;

/**
 * Describes a single field available in SpEL routing expressions and validation rules.
 */
public record SpelFieldDto(
    String path,
    String type,
    boolean isObject
) {}
