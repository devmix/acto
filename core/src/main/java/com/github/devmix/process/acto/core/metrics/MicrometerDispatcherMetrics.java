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

package com.github.devmix.process.acto.core.metrics;

import com.github.devmix.process.acto.DispatcherMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Micrometer backend for {@link DispatcherMetrics}: every active object contributes a set of
 * meters tagged with its identifier.
 *
 * <ul>
 *   <li>{@code acto.messages.processed} — counter of processed messages</li>
 *   <li>{@code acto.messages.rejected} — counter of messages rejected by the overflow policy</li>
 *   <li>{@code acto.message.latency} — timer of per-message processing time</li>
 *   <li>{@code acto.queue.depth} — gauge of the current queue depth</li>
 *   <li>{@code acto.objects.destroyed} — counter of destroyed objects</li>
 * </ul>
 *
 * <p>Meters are created lazily on the first event of an object and cached, so the number of
 * distinct object identifiers must be bounded. This class is only usable when micrometer-core
 * is on the classpath: the dependency is optional.</p>
 *
 * @author Sergey Grachev
 */
public class MicrometerDispatcherMetrics implements DispatcherMetrics {

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, AtomicInteger> queueDepths = new ConcurrentHashMap<>();

    /**
     * Creates metrics bound to the given registry.
     *
     * @param registry the micrometer registry to register meters at
     */
    public MicrometerDispatcherMetrics(final MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void messageProcessed(final Object objectId, final long latencyNanos) {
        final var tags = tags(objectId);
        registry.counter("acto.messages.processed", tags).increment();
        registry.timer("acto.message.latency", tags).record(latencyNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void messageRejected(final Object objectId) {
        registry.counter("acto.messages.rejected", tags(objectId)).increment();
    }

    @Override
    public void queueDepth(final Object objectId, final int depth) {
        final var key = String.valueOf(objectId);
        queueDepths.computeIfAbsent(key, k -> registry.gauge("acto.queue.depth", tags(objectId), new AtomicInteger()))
                .set(depth);
    }

    @Override
    public void objectDestroyed(final Object objectId) {
        registry.counter("acto.objects.destroyed", tags(objectId)).increment();
    }

    private static List<Tag> tags(final Object objectId) {
        return List.of(Tag.of("object", String.valueOf(objectId)));
    }
}
