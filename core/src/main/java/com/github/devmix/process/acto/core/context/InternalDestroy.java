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

package com.github.devmix.process.acto.core.context;

/**
 * Destroy marker message.
 * <ul>
 *     <li>graceful ({@code force == false}): appended to the tail of the queue, so every message
 *     accepted before the destroy request is processed first — no accepted work is lost;</li>
 *     <li>forced ({@code force == true}): inserted at the head of the queue, the remaining queued
 *     messages are discarded and their futures completed exceptionally.</li>
 * </ul>
 *
 * @author Sergey Grachev
 */
public final class InternalDestroy {

    private static final InternalDestroy GRACEFUL = new InternalDestroy(false);
    private static final InternalDestroy FORCE = new InternalDestroy(true);

    private final boolean force;

    private InternalDestroy(final boolean force) {
        this.force = force;
    }

    /**
     * Graceful destroy: the object finishes all queued messages before being destroyed.
     */
    public static InternalDestroy instance() {
        return GRACEFUL;
    }

    /**
     * Forced destroy: queued messages are discarded without processing.
     */
    public static InternalDestroy force() {
        return FORCE;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public String toString() {
        return "InternalDestroy{force=" + force + '}';
    }
}
