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

package com.github.devmix.process.acto.core

import com.github.devmix.process.acto.*

import com.github.devmix.process.acto.core.config.DispatcherConfig
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import com.github.devmix.process.acto.listeners.OnMessageListener
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Sergey Grachev
 */
class MessageDeadlineSpec extends Specification {

    static class Hanging implements OnMessageListener<Hanging> {
        final CountDownLatch release = new CountDownLatch(1)
        final AtomicInteger processed = new AtomicInteger()

        @Override
        Object onObjectMessage(final Object message, final ActiveObjectContext<Hanging> context) {
            if ('hang'.equals(message)) {
                release.await(10, TimeUnit.SECONDS)
            }
            processed.incrementAndGet()
            return message
        }
    }

    def 'a stuck listener times out the request future'() {
        given: 'a message deadline of 200 ms and an object stuck in the handler'
        def config = DispatcherConfig.builder().messageTimeout(Duration.ofMillis(200)).build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def hanging = new Hanging()
        def object = registry.create({ id, options -> hanging }, 'stuck', null)
        object.requestAndForget('hang')
        Thread.sleep(50)

        when: 'another request waits behind the stuck one'
        object.request('later').get(5, TimeUnit.SECONDS)

        then: 'the caller gets TimeoutException without waiting for the stuck handler'
        def error = thrown(ExecutionException)
        error.cause instanceof TimeoutException

        cleanup:
        hanging.release.countDown()
        registry.shutdown(true)
    }

    def 'a cancelled future is never processed'() {
        given: 'an object stuck in the handler'
        def registry = new DefaultActiveObjectsDispatcher()
        def hanging = new Hanging()
        def object = registry.create({ id, options -> hanging }, 'stuck', null)
        object.requestAndForget('hang')
        Thread.sleep(50)

        when: 'a queued request is cancelled before the dispatcher reaches it'
        def future = object.request('later')
        future.cancel(true)
        hanging.release.countDown()
        object.awaitEmptyQueue(5, TimeUnit.SECONDS)

        then: 'the cancelled message was skipped, only the stuck one was processed'
        hanging.processed.get() == 1

        cleanup:
        registry.shutdown(true)
    }

    def 'messageTimeout zero keeps the wait-forever default'() {
        given: 'the default config and a handler that unblocks after 300 ms'
        def registry = new DefaultActiveObjectsDispatcher()
        def hanging = new Hanging()
        def object = registry.create({ id, options -> hanging }, 'stuck', null)
        def releaser = Thread.start {
            Thread.sleep(300)
            hanging.release.countDown()
        }

        when: 'a request is awaited without any explicit timeout'
        def result = object.request('hang').get(5, TimeUnit.SECONDS)
        releaser.join()

        then: 'the request completes normally'
        result == 'hang'

        cleanup:
        registry.shutdown(true)
    }
}
