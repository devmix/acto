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

package com.github.devmix.process.acto.stress

import com.github.devmix.process.acto.ActiveObject
import com.github.devmix.process.acto.ActiveObjectContext
import com.github.devmix.process.acto.ActiveObjectStatus
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import com.github.devmix.process.acto.listeners.OnCreateListener
import com.github.devmix.process.acto.listeners.OnMessageListener
import com.github.devmix.process.acto.messages.Start
import org.junit.experimental.categories.Category
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
@Category(SlowTest.class)
class ConcurrentStartStopSpec extends Specification {

    static final int OBJECTS = 200

    def 'a 200-object chain starts and is destroyed concurrently'() {
        given: 'a forward-only chain n:0 -> n:1 -> ... -> n:199'
        def registry = new DefaultActiveObjectsDispatcher()
        def objects = (0..<OBJECTS).collect { i ->
            def dependency = i + 1 < OBJECTS ? "n:${i + 1}" : null
            registry.create({ id, options -> new ChainObject(dependency) }, "n:${i}".toString(), null)
        }

        when: 'all objects are started concurrently'
        def futures = objects.collect { it.request(Start.standard()) }
        futures.each { it.get(30, TimeUnit.SECONDS) }

        then: 'every object is started'
        objects.every {
            it.status == ActiveObjectStatus.STARTED || it.status == ActiveObjectStatus.ACTIVATED
        }

        when: 'all objects are destroyed concurrently'
        def results = Collections.synchronizedList(new ArrayList<Boolean>())
        def destroyers = objects.collect { obj ->
            Thread.start { results << registry.destroy(obj.id, true) }
        }
        destroyers.each { it.join(30_000) }

        then: 'every object is destroyed and removed'
        results.every { it }
        objects.every { registry.findById(it.id) == null }

        cleanup:
        registry.shutdown(true)
    }

    static class ChainObject implements OnCreateListener<ChainObject>, OnMessageListener<ChainObject> {
        final String dependency

        ChainObject(final String dependency) {
            this.dependency = dependency
        }

        @Override
        void onObjectCreate(ActiveObjectContext<ChainObject> context) {
            if (dependency != null) {
                context.addDependsOn(dependency, false)
            }
        }

        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<ChainObject> context) {
            return 'ping' == message ? 'pong' : null
        }
    }
}
