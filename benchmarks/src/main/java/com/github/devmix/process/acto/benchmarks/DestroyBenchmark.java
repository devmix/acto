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

package com.github.devmix.process.acto.benchmarks;

import com.github.devmix.process.acto.ActiveObject;
import com.github.devmix.process.acto.ActiveObjectsDispatcher;
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher;
import com.github.devmix.process.acto.listeners.OnMessageListener;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cost of {@code destroy()} in its four variants: graceful vs forced, empty vs loaded queue.
 *
 * @author Sergey Grachev
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 20)
@Fork(1)
public class DestroyBenchmark {

    private static final int QUEUE_LOAD = 100;

    public static class Slow implements OnMessageListener<Slow> {
        final AtomicInteger processed = new AtomicInteger();

        @Override
        public Object onObjectMessage(final Object message, final com.github.devmix.process.acto.ActiveObjectContext<Slow> context) {
            if ("slow".equals(message)) {
                try {
                    Thread.sleep(1);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            processed.incrementAndGet();
            return message;
        }
    }

    private ActiveObjectsDispatcher registry;
    private AtomicInteger ids;

    @Setup
    public void setUp() {
        registry = new DefaultActiveObjectsDispatcher();
        ids = new AtomicInteger();
    }

    private ActiveObject<Slow> create(final boolean withLoad) {
        final var id = "obj-" + ids.incrementAndGet();
        final var object = registry.create((oid, options) -> new Slow(), id, null);
        if (withLoad) {
            for (var i = 0; i < QUEUE_LOAD; i++) {
                object.requestAndForget("slow");
            }
        }
        return object;
    }

    @Benchmark
    public boolean gracefulDestroyEmptyQueue() {
        final var object = create(false);
        return registry.destroy(object.getId(), false);
    }

    @Benchmark
    public boolean forcedDestroyEmptyQueue() {
        final var object = create(false);
        return registry.destroy(object.getId(), true);
    }

    @Benchmark
    public boolean gracefulDestroyLoadedQueue() {
        final var object = create(true);
        return registry.destroy(object.getId(), false);
    }

    @Benchmark
    public boolean forcedDestroyLoadedQueue() {
        final var object = create(true);
        return registry.destroy(object.getId(), true);
    }
}
