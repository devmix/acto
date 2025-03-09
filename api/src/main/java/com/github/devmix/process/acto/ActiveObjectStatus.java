package com.github.devmix.process.acto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the possible states of an Active Object in its lifecycle.
 *
 * @author Sergey Grachev
 */
@Getter
@RequiredArgsConstructor
public enum ActiveObjectStatus {

    /**
     * The initial state of an Active Object, indicating that it has been created but not yet started.
     */
    CREATED(false),

    /**
     * The state where the Active Object is in the process of starting up and initializing.
     */
    CREATING(true),

    /**
     * The state of an Active Object after it has completed its startup sequence.
     */
    STARTED(false),

    /**
     * The state where the Active Object is transitioning from the CREATED or STOPPED states to the STARTED state.
     */
    STARTING(true),

    /**
     * The state of an Active Object indicating that it is ready to handle messages and perform its operations.
     */
    ACTIVATED(false),

    /**
     * The state where the Active Object is in the process of transitioning from the ACTIVATED state to DEACTIVATED.
     */
    ACTIVATING(true),

    /**
     * The state of an Active Object indicating that it has ceased handling messages and is not performing its operations.
     */
    DEACTIVATED(false),

    /**
     * The state where the Active Object is in the process of transitioning from the ACTIVATED state to DEACTIVATED.
     */
    DEACTIVATING(true),
    /**
     * The state of an Active Object indicating that it is shutting down and preparing for destruction.
     */
    STOPPED(false),

    /**
     * The state where the Active Object is in the process of transitioning from the ACTIVATED or DEACTIVATED states to the STOPPED state.
     */
    STOPPING(true),

    /**
     * The final terminal state of an Active Object, indicating that it has been fully destroyed and no longer exists.
     */
    DESTROYED(false),

    /**
     * The state where the Active Object is in the process of transitioning from the STOPPED state to the DESTROYED state.
     */
    DESTROYING(true);

    /**
     * Indicates whether this state is a transient state or not. Transient states are temporary and represent a transition between two stable states.
     */
    private final boolean isTransient;
}
