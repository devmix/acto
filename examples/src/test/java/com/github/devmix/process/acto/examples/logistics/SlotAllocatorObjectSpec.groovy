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
class SlotAllocatorObjectSpec extends Specification {

    def 'a slot never takes more bookings than its capacity'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def slots = registry.create({ id, options -> new SlotAllocatorObject(id, options) }, 'slots', 3)

        when: '10 customers race for one slot'
        def results = (1..10).collect { slots.request(new SlotAllocatorObject.Book('morning', "c$it")) }
                *.get(5, TimeUnit.SECONDS)

        then: 'exactly capacity bookings succeed'
        results.count { it == Boolean.TRUE } == 3
        slots.<Integer>request(new SlotAllocatorObject.Taken('morning')).get(5, TimeUnit.SECONDS) == 3

        cleanup:
        registry.shutdown(true)
    }
}
