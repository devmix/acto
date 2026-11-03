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
class RiskLimitObjectSpec extends Specification {

    def 'the exposure limit is never breached by racing trades'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def risk = registry.create({ id, options -> new RiskLimitObject(id, options) }, 'risk', 100L)

        when: '50 trades of 10 race through the limit'
        def results = (1..50).collect { risk.request(new RiskLimitObject.Trade(10L)) }
                *.get(5, TimeUnit.SECONDS)

        then: 'exactly 10 are accepted and the exposure sits on the limit'
        results.count { it == Boolean.TRUE } == 10
        risk.<Long>request(new RiskLimitObject.Exposure()).get(5, TimeUnit.SECONDS) == 100L

        cleanup:
        registry.shutdown(true)
    }
}
