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
class ShipmentTrackerObjectSpec extends Specification {

    def 'events arriving out of order are applied strictly in sequence order'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def tracker = registry.create({ id, options -> new ShipmentTrackerObject() }, 'ship', null)

        when: 'events 3, 1, 2 arrive in that order'
        tracker.request(new ShipmentTrackerObject.Event(3, 'IN_TRANSIT')).get(5, TimeUnit.SECONDS)
        tracker.request(new ShipmentTrackerObject.Event(1, 'CREATED')).get(5, TimeUnit.SECONDS)
        tracker.request(new ShipmentTrackerObject.Event(2, 'PICKED')).get(5, TimeUnit.SECONDS)

        then: 'the history is in sequence order'
        tracker.<List<String>>request(new ShipmentTrackerObject.History()).get(5, TimeUnit.SECONDS) == ['CREATED', 'PICKED', 'IN_TRANSIT']

        and: 'nothing is left buffered'
        tracker.<Integer>request(new ShipmentTrackerObject.Buffered()).get(5, TimeUnit.SECONDS) == 0

        cleanup:
        registry.shutdown(true)
    }
}
