package com.github.tobi.laa.reflective.fluent.builders.exception;


/**
 * <p>
 * Thrown when accessing classes, methods, fields or the like via reflection yields an error.
 * </p>
 */
public class ReflectionException extends RuntimeException {

    private static final long serialVersionUID = -258353301087202952L;

    public ReflectionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
