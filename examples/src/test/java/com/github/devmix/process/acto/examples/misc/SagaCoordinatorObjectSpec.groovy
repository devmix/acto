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

package com.github.devmix.process.acto.examples.misc

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class SagaCoordinatorObjectSpec extends Specification {

    def 'a failed step compensates the completed steps in reverse order'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def saga = registry.create({ id, options -> new SagaCoordinatorObject() }, 'saga', null)

        when: 'steps 1 and 2 succeed and step 3 fails'
        saga.request(new SagaCoordinatorObject.StepOk(1)).get(5, TimeUnit.SECONDS)
        saga.request(new SagaCoordinatorObject.StepOk(2)).get(5, TimeUnit.SECONDS)
        saga.request(new SagaCoordinatorObject.StepFail(3)).get(5, TimeUnit.SECONDS)

        then: 'the saga compensates in reverse'
        saga.<String>request(new SagaCoordinatorObject.State()).get(5, TimeUnit.SECONDS) == 'COMPENSATED'
        saga.<List<String>>request(new SagaCoordinatorObject.Compensations()).get(5, TimeUnit.SECONDS) == ['compensate:2', 'compensate:1']

        cleanup:
        registry.shutdown(true)
    }

    def 'all steps succeeding leave the saga running without compensations'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def saga = registry.create({ id, options -> new SagaCoordinatorObject() }, 'saga', null)

        when: 'every step succeeds'
        (1..3).collect { saga.request(new SagaCoordinatorObject.StepOk(it)) }*.get(5, TimeUnit.SECONDS)

        then:
        saga.<String>request(new SagaCoordinatorObject.State()).get(5, TimeUnit.SECONDS) == 'RUNNING'
        saga.<List<String>>request(new SagaCoordinatorObject.Compensations()).get(5, TimeUnit.SECONDS).isEmpty()

        cleanup:
        registry.shutdown(true)
    }
}
