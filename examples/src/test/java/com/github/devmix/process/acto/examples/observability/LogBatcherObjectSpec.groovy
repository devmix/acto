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

package com.github.devmix.process.acto.examples.observability

import com.github.devmix.process.acto.core.config.DispatcherConfig
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class LogBatcherObjectSpec extends Specification {

    def 'the batch flushes on size and the quiet period flushes the remainder'() {
        given:
        def config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def batcher = registry.create({ id, options -> new LogBatcherObject(id, options) }, 'batcher', 5)

        when: '7 lines arrive'
        (1..7).collect { batcher.request(new LogBatcherObject.Log("line-$it")) }*.get(5, TimeUnit.SECONDS)

        then: 'the full batch flushed immediately, the tail is pending'
        def batches = batcher.<List<List<String>>>request(new LogBatcherObject.Batches()).get(5, TimeUnit.SECONDS)
        batches.size() == 1
        batches.first().size() == 5
        batcher.<Integer>request(new LogBatcherObject.Pending()).get(5, TimeUnit.SECONDS) == 2

        when: 'the stream goes quiet'
        Thread.sleep(500)

        then: 'the heartbeat flushed the tail'
        batcher.<List<List<String>>>request(new LogBatcherObject.Batches()).get(5, TimeUnit.SECONDS).size() == 2
        batcher.<Integer>request(new LogBatcherObject.Pending()).get(5, TimeUnit.SECONDS) == 0

        cleanup:
        registry.shutdown(true)
    }
}
