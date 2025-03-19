/*
 * devMix · Process · Active Objects [ActO]
 * Copyright (C) 2025, Sergey Grachev <sergey.grachev@yahoo.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
        /**
         * Default constructor for creating an {@link Activate} message instance.
         */
        public Activate() {}
    }

    /**
     * Represents an Active Object message indicating a request to deactivate,
     * transitioning from active to idle state. The transition can be forced
     * immediately if specified.
     *
     * @see Deactivate#force
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









