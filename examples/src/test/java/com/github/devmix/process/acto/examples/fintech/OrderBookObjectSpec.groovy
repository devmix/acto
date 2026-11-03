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
class OrderBookObjectSpec extends Specification {

    def 'the book stays consistent under concurrent adds and cancels'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def book = registry.create({ id, options -> new OrderBookObject() }, 'book', null)

        when: 'bids and asks are added concurrently'
        def adds = (1..50).collectMany { i ->
            [book.request(new OrderBookObject.Add("b$i", true, 100 + i, 1)),
             book.request(new OrderBookObject.Add("a$i", false, 200 + i, 1))]
        }
        adds*.get(5, TimeUnit.SECONDS)

        then: 'best bid and ask are correct'
        def best = book.<Map<String, Long>>request(new OrderBookObject.Best()).get(5, TimeUnit.SECONDS)
        best.bid == 150
        best.ask == 201

        when: 'the best orders are cancelled'
        book.request(new OrderBookObject.Cancel('b50')).get(5, TimeUnit.SECONDS)
        book.request(new OrderBookObject.Cancel('a1')).get(5, TimeUnit.SECONDS)

        then:
        def next = book.<Map<String, Long>>request(new OrderBookObject.Best()).get(5, TimeUnit.SECONDS)
        next.bid == 149
        next.ask == 202

        cleanup:
        registry.shutdown(true)
    }
}
