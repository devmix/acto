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

package com.github.devmix.process.acto.examples.logistics

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class VehiclePoolObjectSpec extends Specification {

    def 'expired leases are reclaimed lazily, without a timer thread'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def pool = registry.create({ id, options -> new VehiclePoolObject(id, options) }, 'pool', 100L)

        when: 'three vehicles are borrowed'
        def borrowed = (1..3).collect { pool.<String>request(new VehiclePoolObject.Borrow("c$it")).get(5, TimeUnit.SECONDS) }

        then: 'all three are on lease'
        pool.<Integer>request(new VehiclePoolObject.Free()).get(5, TimeUnit.SECONDS) == 3

        when: 'one is returned'
        pool.request(new VehiclePoolObject.Return(borrowed.first())).get(5, TimeUnit.SECONDS)

        then:
        pool.<Integer>request(new VehiclePoolObject.Free()).get(5, TimeUnit.SECONDS) == 2

        when: 'the lease time passes'
        Thread.sleep(200)

        then: 'the next interaction reclaims the expired leases'
        pool.<Integer>request(new VehiclePoolObject.Free()).get(5, TimeUnit.SECONDS) == 0

        cleanup:
        registry.shutdown(true)
    }
}
