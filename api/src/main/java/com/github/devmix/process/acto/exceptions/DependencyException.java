package com.github.devmix.process.acto.exceptions;

import java.io.Serial;

/**
 * Exception thrown to indicate a problem with dependency resolution during lifecycle operations.
 *
 * @author Sergey Grachev
 */
public final class DependencyException extends LifecycleException {

    @Serial
    private static final long serialVersionUID = -3238400864183549957L;

    /**
     * Constructs a new <code>DependencyException</code> with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public DependencyException(final String message) {
        super(message);
    }

    /**
     * Constructs a new <code>DependencyException</code> with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *                (A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public DependencyException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
