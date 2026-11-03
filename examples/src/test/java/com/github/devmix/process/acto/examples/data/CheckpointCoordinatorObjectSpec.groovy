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

package com.github.devmix.process.acto.examples.data

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class CheckpointCoordinatorObjectSpec extends Specification {

    def 'graceful destroy guarantees every accepted record reached the sink'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def coordinator = registry.create({ id, options -> new CheckpointCoordinatorObject() }, 'sink', null)

        when: '100 records are accepted concurrently'
        def futures = (0..<100).collect { coordinator.request(new CheckpointCoordinatorObject.Process(it, "r$it")) }
        futures*.get(5, TimeUnit.SECONDS)
        def watermark = coordinator.<Long>request(new CheckpointCoordinatorObject.Watermark()).get(5, TimeUnit.SECONDS)

        and: 'the coordinator is destroyed gracefully'
        registry.destroy('sink', false)

        then: 'the sink holds every accepted record'
        coordinator.instance.sink.size() == 100
        watermark == 99L

        cleanup:
        registry.shutdown(true)
    }
}
