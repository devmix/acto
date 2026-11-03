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
class DlqRetryObjectSpec extends Specification {

    def 'items are retried only after their backoff has elapsed'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def dlq = registry.create({ id, options -> new DlqRetryObject(id, options) }, 'dlq', 150L)

        when: 'three items are enqueued and flushed immediately'
        (1..3).collect { dlq.request(new DlqRetryObject.Enqueue("d$it", 'payload')) }*.get(5, TimeUnit.SECONDS)
        dlq.request(new DlqRetryObject.Flush()).get(5, TimeUnit.SECONDS)

        then: 'nothing is due yet'
        dlq.<Integer>request(new DlqRetryObject.Pending()).get(5, TimeUnit.SECONDS) == 3

        when: 'the backoff passes and the queue is flushed again'
        Thread.sleep(250)
        dlq.request(new DlqRetryObject.Flush()).get(5, TimeUnit.SECONDS)

        then: 'all due items were retried'
        dlq.<Integer>request(new DlqRetryObject.Pending()).get(5, TimeUnit.SECONDS) == 0
        dlq.instance.processedView().size() == 3

        cleanup:
        registry.shutdown(true)
    }
}
