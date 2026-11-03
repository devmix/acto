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

package com.github.devmix.process.acto

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import com.github.devmix.process.acto.listeners.OnDestroyListener
import com.github.devmix.process.acto.listeners.OnMessageListener
import com.github.devmix.process.acto.messages.Stop
import spock.lang.Specification

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Sergey Grachev
 */
class QueueDispatchingSpec extends Specification {

    def 'destroy during a long message processing does not create a second consumer'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def tracker = new ConcurrencyTracker(200)
        def object = registry.create({ id, options -> tracker }, 'obj-1', null)
        object.request('warmup').get() == 'pong'

        when: 'a slow message occupies the dispatch thread'
        def slow = object.request('slow')
        Thread.sleep(50)

        and: 'destroy is requested while the slow message is being processed'
        def destroyed = registry.destroy('obj-1', true)

        then:
        destroyed
        slow.get(5, TimeUnit.SECONDS) == 'pong'
        tracker.violations.get() == 0

        cleanup:
        registry.shutdown(true)
    }

    def 'force stop is processed before queued messages'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def tracker = new ConcurrencyTracker(50)
        def object = registry.create({ id, options -> tracker }, 'obj-1', null)
        object.request('warmup').get() == 'pong'

        when: 'queue several slow messages'
        (1..5).each { object.requestAndForget('slow') }
        Thread.sleep(10)

        and: 'force stop jumps the queue'
        def stop = object.request(Stop.force())

        then:
        stop.get(5, TimeUnit.SECONDS) == Boolean.TRUE
        // warmup + the in-flight slow message; the rest are rejected after STOPPED
        tracker.processed.get() == 2
        tracker.violations.get() == 0

        cleanup:
        registry.shutdown(true)
    }

    def 'awaitEmptyQueue waits until all queued messages are processed'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def tracker = new ConcurrencyTracker(0)
        def object = registry.create({ id, options -> tracker }, 'obj-1', null)
        object.request('warmup').get() == 'pong'
        def total = 1_000

        when:
        total.times { object.requestAndForget('ping') }
        object.awaitEmptyQueue(10, TimeUnit.SECONDS)

        then:
        tracker.processed.get() == total + 1

        cleanup:
        registry.shutdown(true)
    }

    def 'awaitEmptyQueue returns immediately on empty queue'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def tracker = new ConcurrencyTracker(0)
        def object = registry.create({ id, options -> tracker }, 'obj-1', null)
        object.request('warmup').get() == 'pong'

        when:
        def start = System.nanoTime()
        object.awaitEmptyQueue(5, TimeUnit.SECONDS)
        def elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)

        then:
        elapsedMillis < 100

        cleanup:
        registry.shutdown(true)
    }

    def 'awaitEmptyQueue throws TimeoutException while messages are still processing'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def tracker = new ConcurrencyTracker(5)
        def object = registry.create({ id, options -> tracker }, 'obj-1', null)
        object.request('warmup').get() == 'pong'

        when: 'the queue holds far more work than the timeout'
        1_000.times { object.requestAndForget('slow') }
        object.awaitEmptyQueue(200, TimeUnit.MILLISECONDS)

        then:
        thrown(TimeoutException)

        cleanup:
        registry.shutdown(true)
    }

    // ------------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------------

    static class ConcurrencyTracker implements OnMessageListener<ConcurrencyTracker>, OnDestroyListener<ConcurrencyTracker> {
        final AtomicInteger inFlight = new AtomicInteger(0)
        final AtomicInteger violations = new AtomicInteger(0)
        final AtomicInteger processed = new AtomicInteger(0)
        final long slowMillis

        ConcurrencyTracker(final long slowMillis) {
            this.slowMillis = slowMillis
        }

        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<ConcurrencyTracker> context) {
            enter()
            try {
                if (message == 'slow' && slowMillis > 0) {
                    Thread.sleep(slowMillis)
                }
                processed.incrementAndGet()
            } finally {
                exit()
            }
            return 'pong'
        }

        @Override
        void onObjectDestroy(ActiveObjectContext<ConcurrencyTracker> context) {
            enter()
            try {
                Thread.sleep(slowMillis)
            } finally {
                exit()
            }
        }

        private void enter() {
            if (inFlight.incrementAndGet() > 1) {
                violations.incrementAndGet()
            }
        }

        private void exit() {
            inFlight.decrementAndGet()
        }
    }
}
