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

package com.github.devmix.process.acto.examples.fintech

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class CircuitBreakerObjectSpec extends Specification {

    def 'no request slips through after the breaker trips'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def breaker = registry.create({ id, options -> new CircuitBreakerObject() }, 'breaker', null)

        when: 'a trip races with a burst of requests'
        def futures = (1..100).collect { breaker.request(new CircuitBreakerObject.Request()) }
        breaker.request(new CircuitBreakerObject.Trip()).get(5, TimeUnit.SECONDS)
        def after = (1..100).collect { breaker.request(new CircuitBreakerObject.Request()) }
                        *.get(5, TimeUnit.SECONDS)

        then: 'everything queued after the trip is rejected'
        after.every { it == 'REJECTED' }

        when: 'the breaker is reset'
        breaker.request(new CircuitBreakerObject.Reset()).get(5, TimeUnit.SECONDS)

        then:
        breaker.<String>request(new CircuitBreakerObject.Request()).get(5, TimeUnit.SECONDS) == 'OK'

        cleanup:
        registry.shutdown(true)
    }
}
