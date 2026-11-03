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

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import com.github.devmix.process.acto.listeners.OnCreateListener
import com.github.devmix.process.acto.listeners.OnMessageListener
import com.github.devmix.process.acto.messages.Start
import com.github.devmix.process.acto.messages.Stop
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Sergey Grachev
 */
class StateVisibilitySpec extends Specification {

    def 'Start.downstream copies chain and visited'() {
        given:
        def o1 = Mock(ActiveObject) { getId() >> 'o1' }
        def o2 = Mock(ActiveObject) { getId() >> 'o2' }
        def origin = Start.standard()
        origin.starting(o1)

        when:
        def copy = Start.downstream(o1, origin, false)
        copy.starting(o2)

        then:
        copy.isVisited('o1')
        copy.isVisited('o2')
        !origin.isVisited('o2')
        origin.chain.size() == 1
    }

    def 'Stop.upstream copies chain'() {
        given:
        def o1 = Mock(ActiveObject) { getId() >> 'o1' }
        def o2 = Mock(ActiveObject) { getId() >> 'o2' }
        def origin = Stop.force()
        origin.stopping(o1)

        when:
        def copy = Stop.upstream(o1, origin)
        copy.stopping(o2)

        then:
        origin.chain.size() == 1
        copy.chain.size() == 2
    }

    def 'concurrent readers of dependencies never observe broken state'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def object = registry.create({ id, options -> new PlainObject() }, 'obj-1', null)
        object.request('warmup').get() == 'pong'

        def errors = Collections.synchronizedList(new ArrayList<Throwable>())
        def stopSignal = new AtomicBoolean(false)

        and: 'reader threads iterate the dependency map'
        def readers = (1..4).collect { r ->
            Thread.start {
                try {
                    while (!stopSignal.get()) {
                        object.forEachDependency({ d -> }, null)
                        object.toString()
                        object.findDependsOn('dep-' + (r % 100))
                    }
                } catch (Throwable t) {
                    errors << t
                }
            }
        }

        when: 'the dependency map is concurrently mutated'
        def writer = Thread.start {
            try {
                500.times { i -> object.addDependsOn('dep-' + i, true) }
            } catch (Throwable t) {
                errors << t
            }
        }
        writer.join(10_000)
        stopSignal.set(true)
        readers.each { it.join(5_000) }

        then:
        errors.every { !(it instanceof ConcurrentModificationException) }
        errors.every { !(it instanceof IndexOutOfBoundsException) }
        errors.every { !(it instanceof NullPointerException) }
        errors.empty

        cleanup:
        registry.shutdown(true)
    }

    def 'status transitions are visible from other threads'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def object = registry.create({ id, options -> new PlainObject() }, 'obj-1', null)
        def errors = Collections.synchronizedList(new ArrayList<Throwable>())
        def observed = Collections.synchronizedList(new ArrayList<ActiveObjectStatus>())

        when: 'the object is started before the reader thread is launched'
        object.request(Start.standard()).get()

        and: 'the reader thread polls the status while the object is stopped'
        def reader = Thread.start {
            try {
                200.times {
                    observed << object.status
                    Thread.sleep(1)
                }
            } catch (Throwable t) {
                errors << t
            }
        }
        Thread.sleep(20)
        object.request(Stop.force()).get()
        reader.join(10_000)

        then:
        errors.empty
        observed.contains(ActiveObjectStatus.STARTED)
        object.status == ActiveObjectStatus.STOPPED

        cleanup:
        registry.shutdown(true)
    }

    def 'fan-out start: shared dependency is started exactly once'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def a = registry.create({ id, options -> new DepObject(['b', 'c']) }, 'a', null)
        def b = registry.create({ id, options -> new DepObject(['d']) }, 'b', null)
        def c = registry.create({ id, options -> new DepObject(['d']) }, 'c', null)
        def d = registry.create({ id, options -> new DepObject([]) }, 'd', null)

        when:
        a.request(Start.standard()).get()

        then:
        new PollingConditions(timeout: 5).eventually {
            assert a.status == ActiveObjectStatus.STARTED
            assert b.status == ActiveObjectStatus.STARTED
            assert c.status == ActiveObjectStatus.STARTED
            assert d.status == ActiveObjectStatus.STARTED
        }
        // d is started exactly once: the second incoming Start is ignored,
        // so d records REQUIRED_FOR only for the object that actually started it
        (d.findRequiredFor('b') != null) ^ (d.findRequiredFor('c') != null)

        cleanup:
        registry.shutdown(true)
    }

    def 'cyclic dependency is rejected on start'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def a = registry.create({ id, options -> new DepObject(['b']) }, 'a', null)
        registry.create({ id, options -> new DepObject(['a']) }, 'b', null)

        when:
        def error = null
        try {
            a.request(Start.standard()).get()
        } catch (final ExecutionException ex) {
            error = ex
        }

        then:
        error != null
        def flat = ''
        for (def t = error; t != null; t = t.cause) {
            flat += String.valueOf(t.message) + '|'
        }
        flat.contains('Cyclic dependency')

        cleanup:
        registry.shutdown(true)
    }

    // ------------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------------

    static class PlainObject implements OnMessageListener<PlainObject> {
        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<PlainObject> context) {
            return 'ping' == message ? 'pong' : null
        }
    }

    static class DepObject implements OnCreateListener<DepObject>, OnMessageListener<DepObject> {
        final List<String> dependsOn

        DepObject(final List<String> dependsOn) {
            this.dependsOn = dependsOn
        }

        @Override
        void onObjectCreate(ActiveObjectContext<DepObject> context) {
            dependsOn.each { context.addDependsOn(it, false) }
        }

        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<DepObject> context) {
            return 'ping' == message ? 'pong' : null
        }
    }
}
