package com.github.devmix.process.acto.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents an Active Object message for controlling idle state transitions.
 *
 * @author Sergey Grachev
 */
public final class Idle {

    private static final Activate ACTIVATE = new Activate();
    private static final Deactivate DEACTIVATE_NOW = new Deactivate(true);

    private Idle() {
    }

    /**
     * Returns a singleton instance of the {@link Activate} message, indicating
     * that the Active Object should transition from idle to active state.
     *
     * @return the singleton {@link Activate} message
     */
    public static Activate activate() {
        return ACTIVATE;
    }

    /**
     * Creates and returns an instance of the {@link Deactivate} message,
     * indicating that the Active Object should transition from active to idle
     * state without forcing the transition.
     *
     * @return a new instance of the {@link Deactivate} message with force set to false
     */
    public static Deactivate deactivate() {
        return new Deactivate(false);
    }

    /**
     * Returns a singleton instance of the {@link Deactivate} message, indicating
     * that the Active Object should transition from active to idle state and
     * forcing the transition immediately.
     *
     * @return the singleton {@link Deactivate} message with force set to true
     */
    public static Deactivate deactivateNow() {
        return DEACTIVATE_NOW;
    }

    /**
     * Represents an Active Object message indicating a request to activate,
     * transitioning from idle to active state.
     */
    public static final class Activate {
    }

    /**
     * Represents an Active Object message indicating a request to deactivate,
     * transitioning from active to idle state. The transition can be forced
     * immediately if specified.
     *
     * @see #isForce()
     */
    @RequiredArgsConstructor
    public static final class Deactivate {
        /**
         * Indicates whether the deactivation should be forced immediately.
         */
        @Getter
        private final boolean force;

    }

}









