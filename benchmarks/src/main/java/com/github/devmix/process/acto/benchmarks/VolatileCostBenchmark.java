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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Stand-alone A/B of a plain vs a volatile field write, the question raised in the PR #9 review:
 * how much does the volatile state of the context fields actually cost.
 *
 * @author Sergey Grachev
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class VolatileCostBenchmark {

    public static class PlainHolder {
        private int value;

        public void set(final int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }
    }

    public static class VolatileHolder {
        private volatile int value;

        public void set(final int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }
    }

    private PlainHolder plain;
    private VolatileHolder volatileHolder;

    @Setup
    public void setUp() {
        plain = new PlainHolder();
        volatileHolder = new VolatileHolder();
    }

    @Benchmark
    public void plainWrite(final Blackhole blackhole) {
        for (var i = 0; i < 100; i++) {
            plain.set(i);
        }
        blackhole.consume(plain.get());
    }

    @Benchmark
    public void volatileWrite(final Blackhole blackhole) {
        for (var i = 0; i < 100; i++) {
            volatileHolder.set(i);
        }
        blackhole.consume(volatileHolder.get());
    }
}
