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

package com.github.devmix.process.acto.core.config;

/**
 * The behavior of {@code DefaultActiveObjectsDispatcher} when a new user message arrives
 * while the object queue already holds {@code queueCapacity} messages.
 *
 * <p>Only user messages are subject to the policy; lifecycle and system messages
 * (start, stop, heartbeat, destroy) always enter the queue, so a full queue can never
 * prevent an object from being stopped or destroyed.</p>
 *
 * <p>{@code CALLER_RUNS}, as known from {@code ThreadPoolExecutor}, is intentionally not
 * offered: running a message on the producer thread would touch the instance concurrently
 * with the dispatch thread and break the single-threaded guarantee of the pattern.</p>
 *
 * @author Sergey Grachev
 */
public enum QueueOverflowPolicy {

    /**
     * Reject the message: {@code request()} completes the future with
     * {@code QueueOverflowException}, {@code requestAndForget()} throws it to the caller.
     */
    FAIL,

    /**
     * Block the producing thread until the object drains a slot from the queue —
     * natural backpressure from the consumer speed. The wait is interruptible but has no
     * timeout: a stuck message handler blocks producers indefinitely. Messages an object
     * sends to itself bypass the policy, so the dispatch thread can never wait on its own
     * queue.
     */
    BLOCK
}
