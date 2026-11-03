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
import com.github.devmix.process.acto.core.config.DispatcherConfig
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import com.github.devmix.process.acto.exceptions.QueueOverflowException
import com.github.devmix.process.acto.listeners.OnMessageListener
import org.junit.experimental.categories.Category
import spock.lang.Specification

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Sergey Grachev
 */
@Category(SlowTest.class)
class BackpressureStressSpec extends Specification {

    static final int PRODUCERS = 10
    static final int MESSAGES = 1_000

    static class Counting implements OnMessageListener<Counting> {
        final AtomicInteger processed = new AtomicInteger()

        @Override
        Object onObjectMessage(final Object message, final ActiveObjectContext<Counting> context) {
            processed.incrementAndGet()
            return null
        }
    }

    def 'a bounded queue keeps the JVM stable under a producer storm'() {
        given: 'a sink object with a queue of 100 slots'
        def config = DispatcherConfig.builder().queueCapacity(100).build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def counter = new Counting()
        def object = registry.create({ id, options -> counter }, 'sink', null)

        when: '10 producers fire 1000 messages each as fast as they can'
        def accepted = new AtomicInteger()
        def rejected = new AtomicInteger()
        def producers = (1..PRODUCERS).collect {
            Thread.start {
                MESSAGES.times {
                    try {
                        object.requestAndForget('m')
                        accepted.incrementAndGet()
                    } catch (final QueueOverflowException ignored) {
                        rejected.incrementAndGet()
                    }
                }
            }
        }
        producers.each { it.join() }

        then: 'every message is either accepted or rejected'
        accepted.get() + rejected.get() == PRODUCERS * MESSAGES

        and: 'the object-side counter agrees with what the producers saw'
        object.rejectedMessages == rejected.get()

        and: 'everything accepted is eventually processed'
        object.awaitEmptyQueue(60, TimeUnit.SECONDS)
        counter.processed.get() == accepted.get()

        cleanup:
        registry.shutdown(true)
    }
}
