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
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import com.github.devmix.process.acto.listeners.OnCreateListener
import com.github.devmix.process.acto.listeners.OnMessageListener
import com.github.devmix.process.acto.listeners.OnStartListener
import com.github.devmix.process.acto.messages.Start
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class DependencyStartSpec extends Specification {

    def 'cascade start starts all dependencies'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def a = registry.create({ id, options -> new DepObject(['b'], []) }, 'a', null)
        registry.create({ id, options -> new DepObject(['c'], []) }, 'b', null)
        registry.create({ id, options -> new DepObject([], []) }, 'c', null)

        when:
        a.request(Start.standard()).get(5, TimeUnit.SECONDS)

        then:
        a.status == ActiveObjectStatus.STARTED
        registry.getById('b').status == ActiveObjectStatus.STARTED
        registry.getById('c').status == ActiveObjectStatus.STARTED

        cleanup:
        registry.shutdown(true)
    }

    def 'missing optional dependency does not fail the start'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def a = registry.create({ id, options -> new DepObject([], ['ghost']) }, 'a', null)

        when:
        a.request(Start.standard()).get(5, TimeUnit.SECONDS)

        then:
        a.status == ActiveObjectStatus.STARTED
        a.findDependsOn('ghost') != null
        !a.findDependsOn('ghost').resolved
        a.findDependsOn('ghost').lastError != null

        cleanup:
        registry.shutdown(true)
    }

    def 'missing required dependency fails the start and rolls back the status'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def a = registry.create({ id, options -> new DepObject(['ghost'], []) }, 'a', null)

        when:
        a.request(Start.standard()).get(5, TimeUnit.SECONDS)

        then:
        thrown(ExecutionException)
        a.status == ActiveObjectStatus.CREATED

        cleanup:
        registry.shutdown(true)
    }

    def 'cross-start cycle fails fast without consuming the dependency timeout'() {
        given: 'a long dependency timeout and a mutual A <-> B dependency'
        def registry = new DefaultActiveObjectsDispatcher(DispatcherConfig.builder()
                .dependencyStartTimeout(Duration.ofSeconds(5))
                .build())
        def a = registry.create({ id, options -> new DepObject(['b'], []) }, 'a', null)
        def b = registry.create({ id, options -> new DepObject(['a'], []) }, 'b', null)
        def unrelated = registry.create({ id, options -> new DepObject([], []) }, 'x', null)

        when: 'both objects are started independently'
        def fa = a.request(Start.standard())
        def fb = b.request(Start.standard())
        def start = System.nanoTime()
        def failedA = false
        def failedB = false
        try {
            fa.get(5, TimeUnit.SECONDS)
        } catch (ExecutionException ignored) {
            failedA = true
        }
        try {
            fb.get(5, TimeUnit.SECONDS)
        } catch (ExecutionException ignored) {
            failedB = true
        }
        def elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)

        then: 'both starts fail much faster than the 5s timeout'
        failedA
        failedB
        elapsedMillis < 2_000

        and: 'unrelated objects stay responsive'
        unrelated.request('ping').get(1, TimeUnit.SECONDS) == 'pong'

        and: 'the start graph is clean: a normal chain still starts'
        def p = registry.create({ id, options -> new DepObject(['q'], []) }, 'p', null)
        registry.create({ id, options -> new DepObject([], []) }, 'q', null)
        p.request(Start.standard()).get(5, TimeUnit.SECONDS)
        p.status == ActiveObjectStatus.STARTED

        cleanup:
        registry.shutdown(true)
    }

    def 'many slow dependency starts do not block unrelated objects'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def unrelated = registry.create({ id, options -> new DepObject([], []) }, 'x', null)
        unrelated.request('ping').get() == 'pong'

        and: '100 objects whose start takes 100ms each'
        def futures = (1..100).collect { i ->
            registry.create({ id, options -> new SlowStartObject(100) }, "slow-$i".toString(), null)
                    .request(Start.standard())
        }

        when: 'all of them are starting'
        def start = System.nanoTime()
        unrelated.request('ping').get(1, TimeUnit.SECONDS) == 'pong'
        def pingMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)

        and: 'all starts complete'
        futures.each { it.get(10, TimeUnit.SECONDS) }

        then:
        pingMillis < 1_000

        cleanup:
        registry.shutdown(true)
    }

    def 'dependency start timeout comes from config'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher(DispatcherConfig.builder()
                .dependencyStartTimeout(Duration.ofMillis(300))
                .build())
        def a = registry.create({ id, options -> new DepObject(['slow'], []) }, 'a', null)
        registry.create({ id, options -> new SlowStartObject(2_000) }, 'slow', null)

        when:
        def start = System.nanoTime()
        a.request(Start.standard()).get(10, TimeUnit.SECONDS)

        then:
        thrown(ExecutionException)
        def elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
        elapsedMillis < 2_000

        cleanup:
        registry.shutdown(true)
    }

    // ------------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------------

    static class DepObject implements OnCreateListener<DepObject>, OnMessageListener<DepObject> {
        final List<String> dependsOn
        final List<String> optionalDependsOn

        DepObject(final List<String> dependsOn, final List<String> optionalDependsOn) {
            this.dependsOn = dependsOn
            this.optionalDependsOn = optionalDependsOn
        }

        @Override
        void onObjectCreate(ActiveObjectContext<DepObject> context) {
            dependsOn.each { context.addDependsOn(it, false) }
            optionalDependsOn.each { context.addDependsOn(it, true) }
        }

        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<DepObject> context) {
            return 'ping' == message ? 'pong' : null
        }
    }

    static class SlowStartObject implements OnStartListener<SlowStartObject> {
        final long millis

        SlowStartObject(final long millis) {
            this.millis = millis
        }

        @Override
        void onObjectStart(Start message, ActiveObjectContext<SlowStartObject> context) {
            try {
                Thread.sleep(millis)
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt()
            }
        }
    }
}
