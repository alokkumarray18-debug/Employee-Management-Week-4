package com.trainee.employeemanagement.exception;

/** Raised when a call to an external/third-party REST API fails or times out. */
public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message) {
        super(message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
