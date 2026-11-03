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
import com.github.devmix.process.acto.listeners.OnIdleListener
import com.github.devmix.process.acto.listeners.OnMessageListener
import com.github.devmix.process.acto.messages.Idle
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Sergey Grachev
 */
class DispatcherConfigSpec extends Specification {

    def 'standard config has documented defaults'() {
        given:
        def config = DispatcherConfig.standard()

        expect:
        config.heartbeatInterval == Duration.ofMinutes(5)
        config.idleTimeout == Duration.ofMinutes(5)
        config.waitMessagesTimeout == Duration.ofMillis(500)
        config.destroyTimeout == Duration.ofSeconds(15)
        config.dependencyStartTimeout == Duration.ofSeconds(15)
        config.dependencyStopTimeout == Duration.ofSeconds(15)
        !config.debugLifecycleLogging
        config.queueCapacity == 0
        config.queueOverflowPolicy == QueueOverflowPolicy.FAIL
    }

    def 'context inherits timeouts from config'() {
        given:
        def config = DispatcherConfig.builder()
                .idleTimeout(Duration.ofSeconds(7))
                .waitMessagesTimeout(Duration.ofMillis(123))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)

        when:
        def object = registry.create({ id, options -> new IdleObject() }, 'obj-1', null)

        then:
        object.idleTimeout == Duration.ofSeconds(7).toNanos()
        object.waitMessagesTimeout == 123
        registry.config.is(config)

        cleanup:
        registry.shutdown(true)
    }

    def 'idle object is deactivated according to config timeouts'() {
        given:
        def config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def object = registry.create({ id, options -> new IdleObject() }, 'obj-1', null)

        when: 'the object becomes active'
        object.request('ping').get() == 'pong'

        then: 'the heartbeat deactivates it after the idle timeout'
        new PollingConditions(timeout: 3).eventually {
            assert object.instance.deactivated.get()
        }

        cleanup:
        registry.shutdown(true)
    }

    def 'shutdown stops the heartbeat thread'() {
        given: 'heartbeat threads that were already alive before the test'
        def before = liveHeartbeatThreads()
        def registry = new DefaultActiveObjectsDispatcher()

        expect: 'the registry creates its own heartbeat thread'
        new PollingConditions(timeout: 2).eventually {
            assert (liveHeartbeatThreads() - before)
        }

        when:
        registry.shutdown(true)

        then: 'the heartbeat thread of this registry terminates'
        new PollingConditions(timeout: 5).eventually {
            assert !(liveHeartbeatThreads() - before)
        }
    }

    def 'deprecated heartbeat constructor maps to config'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher(1_000)

        expect:
        registry.heartbeatInterval == 1_000
        registry.config.heartbeatInterval == Duration.ofMillis(1_000)

        cleanup:
        registry.shutdown(true)
    }

    // ------------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------------

    private static Set<Thread> liveHeartbeatThreads() {
        Thread.getAllStackTraces().keySet().findAll { it.name == 'acto-heartbeat' && it.alive } as Set
    }

    static class IdleObject implements OnMessageListener<IdleObject>, OnIdleListener<IdleObject> {
        final AtomicBoolean deactivated = new AtomicBoolean(false)

        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<IdleObject> context) {
            return 'ping' == message ? 'pong' : null
        }

        @Override
        boolean onObjectActivate(Idle.Activate message, ActiveObjectContext<IdleObject> context) {
            return true
        }

        @Override
        boolean onObjectDeactivate(Idle.Deactivate message, ActiveObjectContext<IdleObject> context) {
            deactivated.set(true)
            return true
        }
    }
}
