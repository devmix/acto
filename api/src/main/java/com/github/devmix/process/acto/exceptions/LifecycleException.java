package com.github.devmix.process.acto.exceptions;

import java.io.Serial;

/**
 * Abstract exception class for lifecycle-related exceptions in an active object.
 *
 * @author Sergey Grachev
 */
abstract class LifecycleException extends ActiveObjectException {

    @Serial
    private static final long serialVersionUID = -2299680028039001891L;

    /**
     * Constructs a new lifecycle exception with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the getMessage() method).
     */
    LifecycleException(final String message) {
        super(message);
    }

    /**
     * Constructs a new lifecycle exception with the specified detail message and cause.
     *
     * <p>Note that the detail message associated with cause is not automatically incorporated in this
     * exception's detail message.</p>
     *
     * @param message The detail message (which is saved for later retrieval by the getMessage() method).
     * @param cause   The cause (which is saved for later retrieval by the getCause() method). A null value is
     *                permitted, and indicates that the cause is nonexistent or unknown.
     */
    LifecycleException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
