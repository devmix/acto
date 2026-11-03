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
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import com.github.devmix.process.acto.listeners.OnDestroyListener
import com.github.devmix.process.acto.listeners.OnMessageListener
import org.junit.experimental.categories.Category
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.LockSupport

/**
 * @author Sergey Grachev
 */
@Category(SlowTest.class)
class ForceStopUnderLoadSpec extends Specification {

    static final int PRODUCERS = 4
    static final int MESSAGES_PER_PRODUCER = 5_000

    def 'destroy while the queue is busy keeps the single-consumer invariant'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def tracker = new TrackingObject()
        def object = registry.create({ id, options -> tracker }, 'obj-1', null)
        object.request('warmup').get() == 'pong'

        and: 'producers keep the queue busy'
        def producers = (1..PRODUCERS).collect {
            Thread.start { MESSAGES_PER_PRODUCER.times { object.requestAndForget('ping') } }
        }

        when: 'the object is destroyed while messages are still arriving'
        Thread.sleep(50)
        def destroyed = registry.destroy('obj-1', true)
        producers.each { it.join(30_000) }

        then:
        destroyed
        tracker.violations.get() == 0
        tracker.inFlight.get() == 0

        cleanup:
        registry.shutdown(true)
    }

    static class TrackingObject implements OnMessageListener<TrackingObject>, OnDestroyListener<TrackingObject> {
        final AtomicInteger inFlight = new AtomicInteger(0)
        final AtomicInteger violations = new AtomicInteger(0)

        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<TrackingObject> context) {
            enter()
            try {
                LockSupport.parkNanos(100)
            } finally {
                exit()
            }
            return 'pong'
        }

        @Override
        void onObjectDestroy(ActiveObjectContext<TrackingObject> context) {
            enter()
            try {
                LockSupport.parkNanos(100)
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
