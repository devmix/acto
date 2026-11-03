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
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Round-trip cost of the two dispatch styles: fire-and-forget and request/response.
 *
 * @author Sergey Grachev
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class DispatchLatencyBenchmark {

    public static class Echo implements OnMessageListener<Echo> {
        @Override
        public Object onObjectMessage(final Object message, final com.github.devmix.process.acto.ActiveObjectContext<Echo> context) {
            return message;
        }
    }

    private ActiveObjectsDispatcher registry;
    private ActiveObject<Echo> object;

    @Setup
    public void setUp() {
        registry = new DefaultActiveObjectsDispatcher();
        object = registry.create((id, options) -> new Echo(), "echo", null);
    }

    @TearDown
    public void tearDown() {
        registry.destroy("echo", true);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void requestAndForget() {
        object.requestAndForget("m");
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.AverageTime})
    public Object requestRoundTrip() throws ExecutionException, InterruptedException {
        return object.request("m").get();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void requestWithoutAwait(final Blackhole blackhole) {
        final CompletableFuture<Object> future = object.request("m");
        blackhole.consume(future.isDone());
    }
}
