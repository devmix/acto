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
class IdempotentApplierObjectSpec extends Specification {

    def 'redelivered events are applied exactly once'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def applier = registry.create({ id, options -> new IdempotentApplierObject() }, 'feed', null)

        when: 'events 1..50 arrive twice each, interleaved'
        def futures = (1..50).collectMany { i ->
            [applier.request(new IdempotentApplierObject.Apply(i, "v$i")),
             applier.request(new IdempotentApplierObject.Apply(i, "v$i"))]
        }
        futures*.get(5, TimeUnit.SECONDS)

        then: 'each event landed once, duplicates were counted'
        applier.<List<String>>request(new IdempotentApplierObject.Applied()).get(5, TimeUnit.SECONDS) == (1..50).collect { "v$it" }
        applier.<Integer>request(new IdempotentApplierObject.Duplicates()).get(5, TimeUnit.SECONDS) == 50

        cleanup:
        registry.shutdown(true)
    }
}
