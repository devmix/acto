package com.github.devmix.process.acto.exceptions;

import java.io.Serial;

/**
 * Represents an exception that occurs during the dispatching process of an active object.
 *
 * @author Sergey Grachev
 */
public final class DispatchingException extends ActiveObjectException {

    @Serial
    private static final long serialVersionUID = -496097685663449702L;

    /**
     * Constructs a new {@code DispatchingException} with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public DispatchingException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code DispatchingException} with the specified detail message and cause.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause()} method). A null value
     *                is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public DispatchingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
