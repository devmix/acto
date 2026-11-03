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
import com.github.devmix.process.acto.core.metrics.MicrometerDispatcherMetrics
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import com.github.devmix.process.acto.listeners.OnMessageListener
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import spock.lang.Specification

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Sergey Grachev
 */
class DispatcherMetricsSpec extends Specification {

    static class Echo implements OnMessageListener<Echo> {
        final CountDownLatch gate
        final AtomicInteger processed = new AtomicInteger()

        Echo(final CountDownLatch gate = null) {
            this.gate = gate
        }

        @Override
        Object onObjectMessage(final Object message, final ActiveObjectContext<Echo> context) {
            if (gate != null) {
                gate.await()
            }
            processed.incrementAndGet()
            return message
        }
    }

    static class Collector implements DispatcherMetrics {
        final List<Object> processedIds = new CopyOnWriteArrayList()
        final List<Long> latencies = new CopyOnWriteArrayList()
        final List<Integer> depths = new CopyOnWriteArrayList()
        final AtomicInteger rejected = new AtomicInteger()
        final List<Object> destroyedIds = new CopyOnWriteArrayList()

        @Override
        void messageProcessed(final Object objectId, final long latencyNanos) {
            processedIds << objectId
            latencies << latencyNanos
        }

        @Override
        void messageRejected(final Object objectId) {
            rejected.incrementAndGet()
        }

        @Override
        void queueDepth(final Object objectId, final int depth) {
            depths << depth
        }

        @Override
        void objectDestroyed(final Object objectId) {
            destroyedIds << objectId
        }
    }

    def "default configuration uses the no-op metrics"() {
        expect:
        DispatcherConfig.standard().metrics === DispatcherMetrics.NOOP
        DispatcherMetrics.NOOP != null
    }

    def "collector receives processed messages with latency"() {
        given:
        def metrics = new Collector()
        def registry = new DefaultActiveObjectsDispatcher(DispatcherConfig.builder().metrics(metrics).build())
        def object = registry.create({ id, options -> new Echo() }, 'echo', null)

        when:
        (1..10).each { object.request("msg-$it") }
        object.awaitEmptyQueue(30, TimeUnit.SECONDS)

        then:
        metrics.processedIds.every { it == 'echo' }
        metrics.processedIds.size() == 10
        metrics.latencies.every { it >= 0 }
        metrics.depths.max() >= 1
        metrics.depths.min() == 0

        cleanup:
        registry.destroy('echo', true)
    }

    def "collector counts rejections and destroyed objects"() {
        given:
        def gate = new CountDownLatch(1)
        def metrics = new Collector()
        def config = DispatcherConfig.builder()
                .metrics(metrics)
                .queueCapacity(1)
                .queueOverflowPolicy(QueueOverflowPolicy.FAIL)
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def object = registry.create({ id, options -> new Echo(gate) }, 'echo', null)

        when: 'the queue is full and one more message arrives'
        object.request('slow')
        Thread.sleep(50)
        object.request('blocked')
        def overflow = object.request('rejected')

        then: 'the overflow surfaces through the future, not as a synchronous exception'
        overflow.isCompletedExceptionally()
        metrics.rejected.get() == 1

        when:
        gate.countDown()
        object.awaitEmptyQueue(30, TimeUnit.SECONDS)
        registry.destroy('echo', true)

        then:
        metrics.destroyedIds == ['echo']
    }

    def "micrometer backend registers counters, timer and gauge"() {
        given:
        def meterRegistry = new SimpleMeterRegistry()
        def config = DispatcherConfig.builder()
                .metrics(new MicrometerDispatcherMetrics(meterRegistry))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def object = registry.create({ id, options -> new Echo() }, 'echo', null)

        when:
        (1..5).each { object.request("msg-$it") }
        object.awaitEmptyQueue(30, TimeUnit.SECONDS)

        then:
        meterRegistry.get('acto.queue.depth').tag('object', 'echo').gauge().value() == 0.0d

        when:
        registry.destroy('echo', true)

        then:
        meterRegistry.get('acto.messages.processed').tag('object', 'echo').counter().count() == 5.0d
        meterRegistry.get('acto.message.latency').tag('object', 'echo').timer().count() == 5L
        meterRegistry.get('acto.objects.destroyed').tag('object', 'echo').counter().count() == 1.0d

        cleanup:
        meterRegistry.close()
    }

    def "micrometer gauge is removed when the object is destroyed"() {
        given:
        def meterRegistry = new SimpleMeterRegistry()
        def config = DispatcherConfig.builder()
                .metrics(new MicrometerDispatcherMetrics(meterRegistry))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def object = registry.create({ id, options -> new Echo() }, 'echo', null)

        when:
        object.request('m').get(30, TimeUnit.SECONDS)

        then:
        meterRegistry.find('acto.queue.depth').tag('object', 'echo').gauge() != null

        when:
        registry.destroy('echo', true)

        then:
        meterRegistry.find('acto.queue.depth').tag('object', 'echo').gauge() == null
        meterRegistry.find('acto.objects.destroyed').tag('object', 'echo').counter().count() == 1.0d

        cleanup:
        meterRegistry.close()
    }

    def "a throwing metrics implementation does not break message processing"() {
        given: 'a metrics implementation that always explodes'
        def exploding = new DispatcherMetrics() {
            @Override
            void messageProcessed(final Object objectId, final long latencyNanos) {
                throw new IllegalStateException('boom')
            }

            @Override
            void queueDepth(final Object objectId, final int depth) {
                throw new IllegalStateException('boom')
            }

            @Override
            void objectDestroyed(final Object objectId) {
                throw new IllegalStateException('boom')
            }
        }
        def registry = new DefaultActiveObjectsDispatcher(DispatcherConfig.builder().metrics(exploding).build())
        def object = registry.create({ id, options -> new Echo() }, 'echo', null)

        when: 'messages flow through every guarded callback'
        def future = object.request('m')

        then: 'the message is still processed and the future still completes'
        future.get(30, TimeUnit.SECONDS) == 'm'
        object.awaitEmptyQueue(30, TimeUnit.SECONDS)

        when:
        def destroyed = registry.destroy('echo', true)

        then: 'destroy succeeds despite the throwing observer'
        destroyed
        !registry.destroy('echo', true) // already gone: unknown id
    }
}
