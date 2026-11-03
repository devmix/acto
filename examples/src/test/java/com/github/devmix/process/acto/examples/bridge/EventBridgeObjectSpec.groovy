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

package com.github.devmix.process.acto.examples.bridge

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class EventBridgeObjectSpec extends Specification {

    def 'a waiter registered before the event is completed by it'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def bridge = registry.create({ id, options -> new EventBridgeObject() }, 'bridge', null)
        def waiter = new CompletableFuture<EventBridgeObject.Event>()

        when: 'the waiter is registered and the callback pushes an event'
        bridge.requestAndForget(new EventBridgeObject.Await(waiter))
        bridge.requestAndForget(new EventBridgeObject.Event('tick', 42))

        then:
        waiter.get(5, TimeUnit.SECONDS).payload() == 42

        cleanup:
        registry.shutdown(true)
    }

    def 'waiters are completed in registration order, one event per waiter'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def bridge = registry.create({ id, options -> new EventBridgeObject() }, 'bridge', null)
        def waiters = (1..3).collect { new CompletableFuture<EventBridgeObject.Event>() }

        when: 'three waiters are registered, then three events arrive'
        waiters.each { bridge.requestAndForget(new EventBridgeObject.Await(it)) }
        (1..3).each { bridge.requestAndForget(new EventBridgeObject.Event('tick', it)) }

        then: 'each waiter got exactly one event, in order'
        waiters.collect { it.get(5, TimeUnit.SECONDS).payload() } == [1, 2, 3]

        cleanup:
        registry.shutdown(true)
    }
}
