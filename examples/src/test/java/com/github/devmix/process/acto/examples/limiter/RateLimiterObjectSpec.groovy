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

package com.github.devmix.process.acto.examples.limiter

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class RateLimiterObjectSpec extends Specification {

    def 'exactly `capacity` of 20 concurrent acquires succeed'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def limiter = registry.create({ id, options -> new RateLimiterObject(id, options) }, 'limiter', 5)

        when: '20 acquires are issued concurrently'
        def futures = (1..20).collect { limiter.request(new RateLimiterObject.TryAcquire()) }
        def results = futures.collect { it.get(5, TimeUnit.SECONDS) }

        then: 'exactly `capacity` succeed, the rest are rejected'
        results.count { it == Boolean.TRUE } == 5
        results.count { it == Boolean.FALSE } == 15

        and: 'nothing is left available'
        limiter.<Integer>request(new RateLimiterObject.Available()).get(5, TimeUnit.SECONDS) == 0

        when: 'permits are released'
        3.times { limiter.request(new RateLimiterObject.Release()).get(5, TimeUnit.SECONDS) }

        then: 'acquires succeed again'
        (1..3).every { limiter.<Boolean>request(new RateLimiterObject.TryAcquire()).get(5, TimeUnit.SECONDS) }

        cleanup:
        registry.shutdown(true)
    }
}
