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

package com.github.devmix.process.acto.examples.erp

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class InventoryReservationObjectSpec extends Specification {

    def '200 concurrent reservations of 1 unit never oversell a stock of 100'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def stock = registry.create({ id, options -> new InventoryReservationObject(id, options) }, 'stock', 100)

        when:
        def results = (1..200).collect { stock.request(new InventoryReservationObject.Reserve(1)) }
                *.get(5, TimeUnit.SECONDS)

        then: 'exactly the stock size is reserved'
        results.count { it == Boolean.TRUE } == 100
        stock.<Integer>request(new InventoryReservationObject.Available()).get(5, TimeUnit.SECONDS) == 0

        when: 'a reservation is released'
        stock.request(new InventoryReservationObject.Release(5)).get(5, TimeUnit.SECONDS)

        then:
        stock.<Integer>request(new InventoryReservationObject.Available()).get(5, TimeUnit.SECONDS) == 5

        cleanup:
        registry.shutdown(true)
    }
}
