package com.github.devmix.process.acto.exceptions;

import java.io.Serial;

/**
 * Custom exception class representing a registry-related error in the Active Object pattern implementation.
 *
 * @author Sergey Grachev
 */
public class RegistryException extends ActiveObjectException {

    @Serial
    private static final long serialVersionUID = -8011900422294771563L;

    /**
     * Constructs a new {@code RegistryException} with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public RegistryException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code RegistryException} with the specified detail message and cause.
     * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated in
     * this exception's detail message.</p>
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *                A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public RegistryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
