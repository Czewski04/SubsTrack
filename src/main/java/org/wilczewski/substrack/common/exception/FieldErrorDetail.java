package org.wilczewski.substrack.common.exception;

public record FieldErrorDetail(
        String field,
        Object rejectedValue,
        String message
) {}
