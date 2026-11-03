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

package com.github.devmix.process.acto.examples.bi

import com.github.devmix.process.acto.core.config.DispatcherConfig
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class SessionWindowizerObjectSpec extends Specification {

    def 'sessions close together when the traffic goes quiet'() {
        given:
        def config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def windowizer = registry.create({ id, options -> new SessionWindowizerObject() }, 'sessions', null)

        when: 'two users produce events'
        ['alice', 'alice', 'bob', 'bob', 'bob'].each {
            windowizer.request(new SessionWindowizerObject.Event(it)).get(5, TimeUnit.SECONDS)
        }

        then: 'both sessions are active'
        windowizer.<Integer>request(new SessionWindowizerObject.Active()).get(5, TimeUnit.SECONDS) == 2

        when: 'the traffic goes quiet'
        Thread.sleep(500)

        then: 'the heartbeat closed both sessions with their event counts'
        def closed = windowizer.<List<String>>request(new SessionWindowizerObject.Closed()).get(5, TimeUnit.SECONDS)
        closed.toSet() == ['alice:2', 'bob:3'].toSet()

        cleanup:
        registry.shutdown(true)
    }
}
