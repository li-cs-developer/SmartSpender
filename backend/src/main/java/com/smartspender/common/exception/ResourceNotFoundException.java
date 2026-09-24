package com.smartspender.common.exception;

/**
 * Thrown when a requested domain object does not exist or is not visible
 * to the requesting user. Mapped to HTTP 404 by GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException forId(String entity, Long id) {
        return new ResourceNotFoundException(entity + " not found with id " + id);
    }
}