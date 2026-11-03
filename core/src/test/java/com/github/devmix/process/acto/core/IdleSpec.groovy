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
import com.github.devmix.process.acto.listeners.OnDestroyListener
import com.github.devmix.process.acto.listeners.OnIdleListener
import com.github.devmix.process.acto.listeners.OnMessageListener
import com.github.devmix.process.acto.listeners.OnStartListener
import com.github.devmix.process.acto.listeners.OnStopListener
import com.github.devmix.process.acto.messages.Idle
import com.github.devmix.process.acto.messages.Start
import com.github.devmix.process.acto.messages.Stop
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Sergey Grachev
 */
class IdleSpec extends Specification {

    def 'full lifecycle: CREATED - STARTED/ACTIVATED - DEACTIVATED - ACTIVATED - STOPPED - DESTROYED'() {
        given:
        def config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .waitMessagesTimeout(Duration.ofMillis(50))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def object = registry.create({ id, options -> new LifecycleTracker() }, 'obj-1', null)

        expect: 'the object is created but not started'
        object.status == ActiveObjectStatus.CREATED

        when: 'a message arrives'
        object.request('ping').get(5, TimeUnit.SECONDS) == 'pong'

        then: 'the object is started and activated'
        object.status == ActiveObjectStatus.ACTIVATED
        object.instance.started.get() == 1
        object.instance.activated.get() == 1

        when: 'the idle timeout passes without activity'
        new PollingConditions(timeout: 3).eventually {
            assert object.instance.deactivated.get() == 1
        }

        then:
        object.status == ActiveObjectStatus.DEACTIVATED

        when: 'a message arrives again'
        object.request('ping').get(5, TimeUnit.SECONDS) == 'pong'

        then: 'the object is activated again'
        object.status == ActiveObjectStatus.ACTIVATED
        object.instance.activated.get() == 2

        when: 'the object is explicitly started while active'
        object.request(Start.standard()).get(5, TimeUnit.SECONDS)

        then: 'start is ignored'
        object.status == ActiveObjectStatus.ACTIVATED

        when: 'the object is force-stopped'
        object.request(Stop.force()).get(5, TimeUnit.SECONDS) == Boolean.TRUE

        then:
        object.status == ActiveObjectStatus.STOPPED
        object.instance.stopped.get() == 1

        when: 'the object is destroyed'
        registry.destroy('obj-1', true)

        then:
        registry.findById('obj-1') == null
        object.instance.destroyed.get() == 1

        cleanup:
        registry.shutdown(true)
    }

    def 'explicit Start message starts and activates the object'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def object = registry.create({ id, options -> new LifecycleTracker() }, 'obj-1', null)

        when:
        object.request(Start.standard()).get(5, TimeUnit.SECONDS)

        then:
        object.status == ActiveObjectStatus.STARTED
        object.instance.started.get() == 1

        cleanup:
        registry.shutdown(true)
    }

    def 'deactivation is disabled with NO_DEACTIVATION_TIMEOUT'() {
        given:
        def config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def object = registry.create({ id, options -> new LifecycleTracker() }, 'obj-1', null)
        object.request('ping').get(5, TimeUnit.SECONDS) == 'pong'

        when: 'deactivation is switched off and the idle timeout passes'
        object.setIdleTimeout(-2L) // NO_DEACTIVATION_TIMEOUT
        Thread.sleep(400)

        then: 'the object stays activated'
        object.status == ActiveObjectStatus.ACTIVATED
        object.instance.deactivated.get() == 0

        cleanup:
        registry.shutdown(true)
    }

    // ------------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------------

    static class LifecycleTracker implements OnMessageListener<LifecycleTracker>, OnStartListener<LifecycleTracker>,
            OnStopListener<LifecycleTracker>, OnDestroyListener<LifecycleTracker>, OnIdleListener<LifecycleTracker> {

        final AtomicInteger started = new AtomicInteger(0)
        final AtomicInteger stopped = new AtomicInteger(0)
        final AtomicInteger destroyed = new AtomicInteger(0)
        final AtomicInteger activated = new AtomicInteger(0)
        final AtomicInteger deactivated = new AtomicInteger(0)

        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<LifecycleTracker> context) {
            return 'ping' == message ? 'pong' : null
        }

        @Override
        void onObjectStart(Start message, ActiveObjectContext<LifecycleTracker> context) {
            started.incrementAndGet()
        }

        @Override
        void onObjectStop(Stop message, ActiveObjectContext<LifecycleTracker> context) {
            stopped.incrementAndGet()
        }

        @Override
        void onObjectDestroy(ActiveObjectContext<LifecycleTracker> context) {
            destroyed.incrementAndGet()
        }

        @Override
        boolean onObjectActivate(Idle.Activate message, ActiveObjectContext<LifecycleTracker> context) {
            activated.incrementAndGet()
            return true
        }

        @Override
        boolean onObjectDeactivate(Idle.Deactivate message, ActiveObjectContext<LifecycleTracker> context) {
            deactivated.incrementAndGet()
            return true
        }
    }
}
