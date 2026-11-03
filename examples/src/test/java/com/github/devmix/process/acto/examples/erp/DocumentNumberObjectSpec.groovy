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
class DocumentNumberObjectSpec extends Specification {

    def '100 concurrent requests get 100 unique gapless numbers'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def numbers = registry.create({ id, options -> new DocumentNumberObject(id, options) }, 'seq', 'DOC')

        when:
        def futures = (1..100).collect { numbers.request(new DocumentNumberObject.Next()) }
        def issued = futures*.get(5, TimeUnit.SECONDS)

        then: 'no gaps, no duplicates'
        issued.toSet().size() == 100
        issued.sort().first() == 'DOC-000001'
        issued.sort().last() == 'DOC-000100'

        cleanup:
        registry.shutdown(true)
    }
}
