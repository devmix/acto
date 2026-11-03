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

package com.github.devmix.process.acto.stress

import com.github.devmix.process.acto.ActiveObjectContext
import com.github.devmix.process.acto.ActiveObject
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import com.github.devmix.process.acto.listeners.OnMessageListener
import org.junit.experimental.categories.Category
import spock.lang.Specification

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Sergey Grachev
 */
@Category(SlowTest.class)
class ThroughputSpec extends Specification {

    // baseline on a development machine: ~2 seconds for 1_000_000 messages
    static final int THREADS = 10
    static final int MESSAGES_PER_THREAD = 100_000
    static final long BASELINE_MILLIS = 2_000

    def 'one million messages are processed exactly once'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def counter = new CounterObject()
        def object = registry.create({ id, options -> counter }, 'obj-1', null)
        object.request('warmup').get() == 'pong'

        when:
        def start = System.nanoTime()
        def producers = (1..THREADS).collect {
            Thread.start { MESSAGES_PER_THREAD.times { object.requestAndForget('inc') } }
        }
        producers.each { it.join() }
        (object as ActiveObjectContext).awaitEmptyQueue(60, TimeUnit.SECONDS)
        def elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)

        then: 'the counter is exact'
        counter.count.get() == THREADS * MESSAGES_PER_THREAD + 1

        and: 'the throughput does not regress by more than 10x against the baseline'
        elapsedMillis < BASELINE_MILLIS * 10

        cleanup:
        registry.shutdown(true)
    }

    static class CounterObject implements OnMessageListener<CounterObject> {
        final AtomicInteger count = new AtomicInteger(0)

        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<CounterObject> context) {
            count.incrementAndGet()
            return 'ping' == message ? 'pong' : null
        }
    }
}
