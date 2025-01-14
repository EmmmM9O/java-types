/* (C) 2025 */
package com.github.emmmm9o.javatypes.exceptions;

/**
 * Exception thrown when there is an error generating type definitions.
 */
public class TypeGenerationException extends RuntimeException {
    public TypeGenerationException(String message) {
        super(message);
    }

    public TypeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
} 