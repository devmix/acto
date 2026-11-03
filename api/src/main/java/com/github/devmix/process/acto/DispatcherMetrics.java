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

package com.github.devmix.process.acto;

/**
 * Observability hooks for the dispatcher. All methods have empty default implementations,
 * so the default {@link #NOOP} adds no behavior and no allocations to the hot path beyond
 * a virtual call.
 *
 * <p>Implementations must be thread-safe: callbacks come from producer and dispatch threads
 * concurrently, and must never throw — a failing observer must not break message processing.</p>
 *
 * @author Sergey Grachev
 */
public interface DispatcherMetrics {

    /**
     * The default no-op instance: zero overhead for applications without a metrics backend.
     */
    DispatcherMetrics NOOP = new DispatcherMetrics() {
    };

    /**
     * Reports that a message was processed by the object.
     *
     * @param objectId     the identifier of the object that processed the message
     * @param latencyNanos the time the message spent in processing, in nanoseconds
     */
    default void messageProcessed(Object objectId, long latencyNanos) {
    }

    /**
     * Reports that a user message was rejected by the queue overflow policy.
     *
     * @param objectId the identifier of the object whose queue was full
     */
    default void messageRejected(Object objectId) {
    }

    /**
     * Reports the current depth of the object queue after an enqueue or a dequeue.
     *
     * @param objectId the identifier of the object
     * @param depth    the number of messages waiting in the queue
     */
    default void queueDepth(Object objectId, int depth) {
    }

    /**
     * Reports that an active object was destroyed and removed from the registry.
     *
     * @param objectId the identifier of the destroyed object
     */
    default void objectDestroyed(Object objectId) {
    }
}
