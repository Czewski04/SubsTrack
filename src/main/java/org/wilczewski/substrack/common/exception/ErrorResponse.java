package org.wilczewski.substrack.common.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, List.of());
    }

    public static ErrorResponse of(
            int status,
            String error,
            String message,
            String path,
            List<FieldErrorDetail> fieldErrors
    ) {
        return new ErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
    }
}
