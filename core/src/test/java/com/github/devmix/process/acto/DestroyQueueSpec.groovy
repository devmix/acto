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
import com.github.devmix.process.acto.exceptions.DispatchingException
import com.github.devmix.process.acto.listeners.OnMessageListener
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Sergey Grachev
 */
class DestroyQueueSpec extends Specification {

    def 'messages queued before destroy are completed exceptionally, not dispatched'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def blocker = new BlockingObject()
        def object = registry.create({ id, options -> blocker }, 'obj-1', null)
        object.request('warmup').get(5, TimeUnit.SECONDS) == 'pong'

        and: 'the object is busy with a blocking message'
        def blocking = object.request('block')

        and: 'more messages pile up in the queue'
        def queued = (1..3).collect { object.request("q$it") }

        when: 'the object is destroyed while the queue is not empty'
        def releaseLater = Thread.start { Thread.sleep(100); blocker.release.countDown() }
        registry.destroy('obj-1', true)
        releaseLater.join()

        then: 'the queued futures fail with DispatchingException instead of hanging'
        queued.every {
            def failedWithDispatching = false
            try {
                it.get(5, TimeUnit.SECONDS)
            } catch (final ExecutionException e) {
                failedWithDispatching = e.cause instanceof DispatchingException
            }
            failedWithDispatching
        }

        and: 'awaitEmptyQueue returns after destroy'
        object.awaitEmptyQueue(5, TimeUnit.SECONDS)

        cleanup:
        blocker.release.countDown()
        registry.shutdown(true)
    }

    def 'requests after destroy fail fast and do not enter the queue'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def object = registry.create({ id, options -> new EchoLike() }, 'obj-1', null)
        object.request('ping').get(5, TimeUnit.SECONDS) == 'pong'
        registry.destroy('obj-1', true)

        when: 'a request is sent to the destroyed object'
        object.request('late').get(5, TimeUnit.SECONDS)

        then: 'the future fails immediately'
        def e = thrown(ExecutionException)
        e.cause instanceof DispatchingException

        when: 'requestAndForget is sent to the destroyed object'
        object.requestAndForget('late')

        then: 'no exception is thrown and the queue stays empty'
        noExceptionThrown()
        object.awaitEmptyQueue(5, TimeUnit.SECONDS)

        cleanup:
        registry.shutdown(true)
    }

    def 'graceful destroy processes every message accepted before the destroy request'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def counter = new CountingObject()
        def object = registry.create({ id, options -> counter }, 'obj-1', null)
        object.request('warmup').get(5, TimeUnit.SECONDS) == 0

        and: 'the object is busy and more messages pile up in the queue'
        def blocking = object.request('block')
        def queued = (1..3).collect { object.request('inc') }

        when: 'a graceful destroy is requested'
        def releaseLater = Thread.start { Thread.sleep(100); counter.release.countDown() }
        def destroyed = registry.destroy('obj-1', false)
        releaseLater.join()

        then: 'every queued message is processed before the object is destroyed'
        destroyed
        queued.collect { it.get(5, TimeUnit.SECONDS) } == [1, 2, 3]
        counter.count.get() == 3
        object.status == ActiveObjectStatus.DESTROYED

        cleanup:
        counter.release.countDown()
        registry.shutdown(true)
    }

    def 'awaitEmptyQueue completes after a forced destroy with a large backlog'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def blocker = new BlockingObject()
        def object = registry.create({ id, options -> blocker }, 'obj-1', null)
        object.request('warmup').get(5, TimeUnit.SECONDS) == 'pong'
        def blocking = object.request('block')
        (1..1000).each { object.requestAndForget('ping') }

        when: 'the object is force-destroyed and the blocking message finishes'
        def releaseLater = Thread.start { Thread.sleep(50); blocker.release.countDown() }
        registry.destroy('obj-1', true)
        releaseLater.join()

        then: 'the drained backlog does not leave pending messages behind'
        noExceptionThrown()
        object.awaitEmptyQueue(5, TimeUnit.SECONDS)

        cleanup:
        blocker.release.countDown()
        registry.shutdown(true)
    }

    def 'messages accepted after the graceful destroy marker are drained, not processed'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def blocker = new BlockingObject()
        def object = registry.create({ id, options -> blocker }, 'obj-1', null)
        object.request('warmup').get(5, TimeUnit.SECONDS) == 'pong'
        def blocking = object.request('block')

        when: 'a graceful destroy is queued and another message arrives after the marker'
        def destroyThread = Thread.start { registry.destroy('obj-1', false) }
        new PollingConditions(timeout: 2).eventually {
            assert object.toString().contains('queue=1')
        }
        def late = object.request('late')
        blocker.release.countDown()
        destroyThread.join(5000)
        late.get(5, TimeUnit.SECONDS)

        then: 'the late message fails with DispatchingException'
        def e = thrown(ExecutionException)
        e.cause instanceof DispatchingException

        and: 'the queue is empty afterwards'
        object.awaitEmptyQueue(5, TimeUnit.SECONDS)

        cleanup:
        blocker.release.countDown()
        registry.shutdown(true)
    }

    // ------------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------------

    static class BlockingObject implements OnMessageListener<BlockingObject> {
        final CountDownLatch release = new CountDownLatch(1)

        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<BlockingObject> context) {
            if ('block' == message) {
                release.await()
            }
            return 'pong'
        }
    }

    static class CountingObject implements OnMessageListener<CountingObject> {
        final CountDownLatch release = new CountDownLatch(1)
        final AtomicInteger count = new AtomicInteger(0)

        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<CountingObject> context) {
            if ('block' == message) {
                release.await()
                return null
            }
            return 'inc' == message ? count.incrementAndGet() : count.get()
        }
    }

    static class EchoLike implements OnMessageListener<EchoLike> {
        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<EchoLike> context) {
            return 'ping' == message ? 'pong' : null
        }
    }
}
