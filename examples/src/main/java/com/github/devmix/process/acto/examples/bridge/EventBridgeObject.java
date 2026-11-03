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

package com.github.devmix.process.acto.examples.bridge;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;

/**
 * Bridges an external callback API into the active object world: callbacks are pushed in
 * with {@code requestAndForget}, callers await them as {@link CompletableFuture}s. The
 * pending-waiter queue lives inside the object, so completion is serialized by the mailbox
 * and no concurrent collection is needed.
 *
 * @author Sergey Grachev
 */
public class EventBridgeObject implements OnMessageListener<EventBridgeObject> {

    /**
     * An event pushed by the external callback source.
     */
    public record Event(String name, Object payload) {
    }

    /**
     * Registers a future to be completed with the next event.
     */
    public record Await(CompletableFuture<Event> future) {
    }

    private final Deque<CompletableFuture<Event>> waiters = new ArrayDeque<>();

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<EventBridgeObject> context) {
        if (message instanceof Event event) {
            if (!waiters.isEmpty()) {
                waiters.poll().complete(event);
            }
            return null;
        }
        if (message instanceof Await await) {
            waiters.add(await.future());
            return null;
        }
        return null;
    }
}
