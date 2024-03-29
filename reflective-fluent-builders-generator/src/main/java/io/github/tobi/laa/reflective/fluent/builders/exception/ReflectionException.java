package io.github.tobi.laa.reflective.fluent.builders.exception;


import java.io.Serial;

/**
 * <p>
 * Thrown when accessing classes, methods, fields or the like via reflection yields an error.
 * </p>
 */
public class ReflectionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -258353301087202952L;

    public ReflectionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
