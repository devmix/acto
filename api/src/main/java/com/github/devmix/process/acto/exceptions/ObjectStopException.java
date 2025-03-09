package com.github.devmix.process.acto.exceptions;

import java.io.Serial;

/**
 * An exception indicating that an object failed to stop due to a blocking condition or external factor.
 *
 * @author Sergey Grachev
 */
public class ObjectStopException extends LifecycleException {

    @Serial
    private static final long serialVersionUID = -2995189569422394680L;

    /**
     * Constructs an instance of <code>ObjectStopException</code> with the specified detail message and the ID of the object that blocked the stop.
     *
     * @param message     The detail message.
     * @param blockedById The ID of the object that blocked the stop.
     */
    public ObjectStopException(final String message, final Object blockedById) {
        super(message);
    }
}
