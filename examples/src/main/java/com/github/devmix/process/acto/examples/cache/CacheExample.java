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

package com.github.devmix.process.acto.examples.cache;

import com.github.devmix.process.acto.core.config.DispatcherConfig;
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Use case 6: a write-through cache with idle invalidation. Aggressive heartbeat settings
 * make the demo observable: after the idle timeout the heartbeat deactivates the object and
 * the in-memory map is dropped, while the persisted store keeps every write.
 *
 * @author Sergey Grachev
 */
public final class CacheExample {

    private CacheExample() {
    }

    public static void main(final String[] args) throws Exception {
        final var config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .build();
        final var registry = new DefaultActiveObjectsDispatcher(config);
        final var cache = registry.create((id, options) -> new WriteThroughCacheObject(), "cache", null);

        cache.request(new WriteThroughCacheObject.Put("k1", "v1")).get();
        System.out.println("get k1: " + cache.<String>request(new WriteThroughCacheObject.Get("k1")).get());

        Thread.sleep(400);
        System.out.println("after idle, get k1: " + cache.<String>request(new WriteThroughCacheObject.Get("k1")).get());
        System.out.println("deactivations: " + cache.<Integer>request(new WriteThroughCacheObject.Deactivations()).get(5, TimeUnit.SECONDS));
        System.out.println("persisted: " + cache.<java.util.List<String>>request(new WriteThroughCacheObject.Persisted()).get());

        registry.shutdown(true);
    }
}
