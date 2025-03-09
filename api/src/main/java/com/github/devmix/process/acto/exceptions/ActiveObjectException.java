package com.github.devmix.process.acto.exceptions;

import java.io.Serial;

/**
 * Base class for exceptions related to Active Object pattern implementation.
 *
 * @author Sergey Grachev
 */
abstract class ActiveObjectException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -2746666380620673017L;

    /**
     * Constructs a new {@code ActiveObjectException} with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *                {@link #getMessage()} method)
     */
    public ActiveObjectException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ActiveObjectException} with the specified detail message and cause.
     *
     * <p>Note that the detail message associated with {@code cause} is <i>not</i>
     * automatically incorporated in this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *                {@link #getMessage()} method)
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method). A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public ActiveObjectException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
