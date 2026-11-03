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
import com.github.devmix.process.acto.exceptions.RegistryException
import com.github.devmix.process.acto.listeners.OnDestroyListener
import com.github.devmix.process.acto.listeners.OnMessageListener
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Sergey Grachev
 */
class RegistrySpec extends Specification {

    def 'destroy started object returns true and removes it from registry'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def object = registry.create({ id, options -> new TrackingObject() }, 'obj-1', null)
        assert object.request('ping').get() == 'pong'

        when:
        def result = registry.destroy('obj-1', false)

        then:
        result
        registry.findById('obj-1') == null
        object.instance.destroyed.get()
        object.instance.messages.get() == 1

        cleanup:
        registry.shutdown(true)
    }

    def 'destroy created (not started) object returns true and removes it from registry'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        registry.create({ id, options -> new TrackingObject() }, 'obj-1', null)

        when:
        def result = registry.destroy('obj-1', false)

        then:
        result
        registry.findById('obj-1') == null

        cleanup:
        registry.shutdown(true)
    }

    def 'destroy unknown id returns false'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()

        expect:
        !registry.destroy('unknown', false)
        !registry.destroy('unknown', true)

        cleanup:
        registry.shutdown(true)
    }

    def 'repeated destroy returns false'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        registry.create({ id, options -> new TrackingObject() }, 'obj-1', null)

        when:
        def first = registry.destroy('obj-1', false)

        then:
        first

        when:
        def second = registry.destroy('obj-1', false)

        then:
        !second

        cleanup:
        registry.shutdown(true)
    }

    def 'destroy returns false and keeps object in registry when OnDestroyListener fails'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def object = registry.create({ id, options -> new FailingDestroyObject() }, 'obj-1', null)
        object.request('ping').get()

        when:
        def result = registry.destroy('obj-1', false)

        then:
        !result
        registry.findById('obj-1') != null

        when:
        def forced = registry.destroy('obj-1', true)

        then:
        !forced
        registry.findById('obj-1') != null

        cleanup:
        registry.shutdown(true)
    }

    def 'concurrent create with the same id is rejected while object is being destroyed'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def object = registry.create({ id, options -> new SlowDestroyObject() }, 'slow-1', null)
        def destroyResult = null
        def destroyThread = Thread.start { destroyResult = registry.destroy('slow-1', true) }
        new PollingConditions(timeout: 5).eventually {
            assert object.instance.destroyStarted.count == 0L
        }

        when: 'create the same id while destroy is in progress'
        registry.create({ id, options -> new TrackingObject() }, 'slow-1', null)

        then:
        thrown(RegistryException)

        when:
        destroyThread.join(10_000)

        then:
        destroyResult
        registry.findById('slow-1') == null

        cleanup:
        registry.shutdown(true)
    }

    def 'create with the same id succeeds after successful destroy'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        registry.create({ id, options -> new TrackingObject() }, 'obj-1', null)
        assert registry.destroy('obj-1', false)

        when:
        def second = registry.create({ id, options -> new TrackingObject() }, 'obj-1', null)

        then:
        second != null
        second.request('ping').get() == 'pong'

        cleanup:
        registry.shutdown(true)
    }

    def 'shutdown(true) removes all objects from registry'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        registry.create({ id, options -> new TrackingObject() }, 'obj-1', null)
        registry.create({ id, options -> new TrackingObject() }, 'obj-2', null)

        when:
        registry.shutdown(true)

        then:
        registry.findById('obj-1') == null
        registry.findById('obj-2') == null
    }

    def 'shutdown(false) keeps object that cannot be destroyed'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def object = registry.create({ id, options -> new FailingDestroyObject() }, 'obj-1', null)
        object.request('ping').get()

        when:
        registry.shutdown(false)

        then:
        registry.findById('obj-1') != null
    }

    def 'create after shutdown is rejected'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        registry.shutdown(true)

        when:
        registry.create({ id, options -> new TrackingObject() }, 'obj-1', null)

        then:
        thrown(RegistryException)
    }

    // ------------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------------

    static class TrackingObject implements OnMessageListener<TrackingObject>, OnDestroyListener<TrackingObject> {
        final AtomicInteger messages = new AtomicInteger(0)
        final AtomicBoolean destroyed = new AtomicBoolean(false)

        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<TrackingObject> context) {
            messages.incrementAndGet()
            return 'ping' == message ? 'pong' : null
        }

        @Override
        void onObjectDestroy(ActiveObjectContext<TrackingObject> context) {
            destroyed.set(true)
        }
    }

    static class SlowDestroyObject implements OnDestroyListener<SlowDestroyObject> {
        final CountDownLatch destroyStarted = new CountDownLatch(1)

        @Override
        void onObjectDestroy(ActiveObjectContext<SlowDestroyObject> context) {
            destroyStarted.countDown()
            try {
                Thread.sleep(300)
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt()
            }
        }
    }

    static class FailingDestroyObject implements OnMessageListener<FailingDestroyObject>, OnDestroyListener<FailingDestroyObject> {
        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<FailingDestroyObject> context) {
            return 'ping' == message ? 'pong' : null
        }

        @Override
        void onObjectDestroy(ActiveObjectContext<FailingDestroyObject> context) {
            throw new IllegalStateException('cannot destroy me')
        }
    }
}
