/* (C) 2025 */
package com.github.emmmm9o.javatypes.exceptions;

/**
 * Exception thrown when there is an error parsing Java types.
 */
public class TypeParsingException extends RuntimeException {
    public TypeParsingException(String message) {
        super(message);
    }

    public TypeParsingException(String message, Throwable cause) {
        super(message, cause);
    }
} 