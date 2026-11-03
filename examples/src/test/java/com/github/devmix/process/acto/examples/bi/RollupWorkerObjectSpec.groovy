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

import com.github.devmix.process.acto.core.config.DispatcherConfig
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class RollupWorkerObjectSpec extends Specification {

    def 'the current bucket is emitted when the stream goes quiet'() {
        given:
        def config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def rollup = registry.create({ id, options -> new RollupWorkerObject() }, 'rollup', null)

        when: 'ten values arrive'
        (1..10).collect { rollup.request(new RollupWorkerObject.Ingest('cpu', 1.0d)) }*.get(5, TimeUnit.SECONDS)

        and: 'the stream goes quiet'
        Thread.sleep(500)

        then: 'one bucket was emitted with the aggregate'
        rollup.<List<String>>request(new RollupWorkerObject.Buckets()).get(5, TimeUnit.SECONDS) == ['sum=10.0,count=10']

        cleanup:
        registry.shutdown(true)
    }
}
