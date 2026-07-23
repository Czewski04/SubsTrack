package org.wilczewski.substrack.common.exception;

import org.springframework.http.HttpStatus;

public abstract class AppException extends RuntimeException {
    private final HttpStatus status;

    protected AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    protected AppException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
