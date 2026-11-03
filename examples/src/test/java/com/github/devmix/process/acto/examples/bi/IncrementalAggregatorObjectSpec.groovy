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

package com.github.devmix.process.acto.examples.bi

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class IncrementalAggregatorObjectSpec extends Specification {

    def '1000 concurrent adds produce exact quantiles'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def aggregator = registry.create({ id, options -> new IncrementalAggregatorObject() }, 'agg', null)

        when: 'values 1..1000 arrive concurrently'
        (1..1000).collect { aggregator.request(new IncrementalAggregatorObject.Add(it)) }*.get(30, TimeUnit.SECONDS)

        then:
        aggregator.<Integer>request(new IncrementalAggregatorObject.Count()).get(5, TimeUnit.SECONDS) == 1000
        aggregator.<Double>request(new IncrementalAggregatorObject.Quantile(0.5d)).get(5, TimeUnit.SECONDS) == 501.0d

        cleanup:
        registry.shutdown(true)
    }
}
