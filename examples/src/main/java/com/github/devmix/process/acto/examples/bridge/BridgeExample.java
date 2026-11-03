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

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Use case 5: bridging a callback API into the active object world. The external source
 * pushes events with {@code requestAndForget}; callers await them as futures.
 *
 * @author Sergey Grachev
 */
public final class BridgeExample {

    private BridgeExample() {
    }

    public static void main(final String[] args) throws Exception {
        final var registry = new DefaultActiveObjectsDispatcher();
        final var bridge = registry.create((id, options) -> new EventBridgeObject(), "bridge", null);

        // an external callback source: a thread that pushes events as they happen
        final var source = Thread.ofVirtual().start(() -> {
            try {
                for (var i = 1; i <= 3; i++) {
                    Thread.sleep(50);
                    bridge.requestAndForget(new EventBridgeObject.Event("tick", i));
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        for (var i = 1; i <= 3; i++) {
            final var event = new CompletableFuture<EventBridgeObject.Event>();
            bridge.requestAndForget(new EventBridgeObject.Await(event));
            System.out.println("awaited: " + event.get(5, TimeUnit.SECONDS));
        }

        source.join();
        registry.shutdown(true);
    }
}
