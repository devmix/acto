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

import com.github.devmix.process.acto.core.config.DispatcherConfig
import com.github.devmix.process.acto.core.config.QueueOverflowPolicy
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import com.github.devmix.process.acto.exceptions.QueueOverflowException
import com.github.devmix.process.acto.listeners.OnMessageListener
import spock.lang.Specification

import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Sergey Grachev
 */
class BackpressureSpec extends Specification {

    static class Slow implements OnMessageListener<Slow> {
        final long delayMs
        final AtomicInteger processed = new AtomicInteger()

        Slow(final long delayMs) {
            this.delayMs = delayMs
        }

        @Override
        Object onObjectMessage(final Object message, final ActiveObjectContext<Slow> context) {
            if ('slow'.equals(message)) {
                Thread.sleep(delayMs)
            }
            processed.incrementAndGet()
            return message
        }
    }

    def 'capacity 0 keeps the queue unbounded'() {
        given: 'a slow object with the default (unbounded) queue'
        def registry = new DefaultActiveObjectsDispatcher()
        def slow = new Slow(5)
        def object = registry.create({ id, options -> slow }, 'slow', null)

        when: '100 messages are fired as fast as the producers can'
        (1..100).each { object.requestAndForget("m$it") }

        then: 'nothing is rejected and everything is eventually processed'
        object.rejectedMessages == 0
        object.awaitEmptyQueue(30, TimeUnit.SECONDS)
        slow.processed.get() == 100

        cleanup:
        registry.shutdown(true)
    }

    def 'FAIL policy rejects the messages that do not fit the queue'() {
        given: 'a queue of capacity 2 and a message that blocks the dispatch thread'
        def config = DispatcherConfig.builder().queueCapacity(2).build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def slow = new Slow(500)
        def object = registry.create({ id, options -> slow }, 'slow', null)
        object.requestAndForget('slow')
        Thread.sleep(50)

        when: 'the queue fills up'
        def accepted = (1..2).collect { object.request("m$it") }

        and: 'one message more arrives and its future is awaited'
        def overflow = object.request('m3')
        overflow.get(5, TimeUnit.SECONDS)

        then: 'the future of the extra message fails with QueueOverflowException'
        def error = thrown(ExecutionException)
        error.cause instanceof QueueOverflowException

        and: 'the accepted futures are untouched'
        accepted*.get(5, TimeUnit.SECONDS) == ['m1', 'm2']

        and: 'the rejection is counted'
        object.rejectedMessages == 1

        cleanup:
        registry.shutdown(true)
    }

    def 'FAIL policy throws QueueOverflowException to a fire-and-forget producer'() {
        given: 'a queue of capacity 1 occupied by a slow message'
        def config = DispatcherConfig.builder().queueCapacity(1).build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def slow = new Slow(500)
        def object = registry.create({ id, options -> slow }, 'slow', null)
        object.requestAndForget('slow')
        Thread.sleep(50)
        object.requestAndForget('m1')

        when: 'a fire-and-forget message hits the full queue'
        object.requestAndForget('m2')

        then:
        thrown(QueueOverflowException)
        object.rejectedMessages == 1

        cleanup:
        registry.shutdown(true)
    }

    def 'BLOCK policy slows the producer down to the consumer speed'() {
        given: 'a queue of capacity 1 occupied by a slow message'
        def config = DispatcherConfig.builder()
                .queueCapacity(1)
                .queueOverflowPolicy(QueueOverflowPolicy.BLOCK)
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def slow = new Slow(300)
        def object = registry.create({ id, options -> slow }, 'slow', null)
        object.requestAndForget('slow')
        Thread.sleep(50)
        object.requestAndForget('m1')

        when: 'a second producer message has to wait for a free slot'
        def start = System.nanoTime()
        object.requestAndForget('m2')
        def waitedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)

        then: 'the producer was blocked until the consumer drained the queue'
        waitedMillis >= 100

        and: 'nothing was rejected'
        object.rejectedMessages == 0

        cleanup:
        registry.shutdown(true)
    }

    def 'a full queue never prevents the object from being destroyed'() {
        given: 'a queue of capacity 1 occupied by a slow message'
        def config = DispatcherConfig.builder().queueCapacity(1).build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def slow = new Slow(300)
        def object = registry.create({ id, options -> slow }, 'slow', null)
        object.requestAndForget('slow')
        Thread.sleep(50)
        object.requestAndForget('m1')

        when: 'the object is destroyed while the queue is full'
        def destroyed = registry.destroy('slow', false)

        then: 'destroy goes through'
        destroyed
        registry.findById('slow') == null

        cleanup:
        registry.shutdown(true)
    }
}
