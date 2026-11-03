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

package com.github.devmix.process.acto.examples.etl

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class EtlExampleSpec extends Specification {

    def 'one message to the target starts the whole pipeline in dependency order'() {
        given: 'the pipeline is created in reverse order and nothing is started explicitly'
        def registry = new DefaultActiveObjectsDispatcher()
        def startOrder = new CopyOnWriteArrayList<String>()
        registry.create({ id, options -> new TargetEndpoint(id, options) }, EtlExample.ID_TARGET, startOrder)
        registry.create({ id, options -> new TransformerEndpoint(id, options) }, EtlExample.ID_TRANSFORMER, startOrder)
        registry.create({ id, options -> new SourceEndpoint(id, options) }, EtlExample.ID_SOURCE, startOrder)

        when: 'a single load message is sent to the target'
        def loaded = registry.getById(EtlExample.ID_TARGET)
                .request(TargetEndpoint.MSG_LOAD).get(5, TimeUnit.SECONDS)

        then: 'the data moved end-to-end'
        loaded == 3

        and: 'the stages started in dependency order'
        startOrder == [EtlExample.ID_SOURCE, EtlExample.ID_TRANSFORMER, EtlExample.ID_TARGET]

        and: 'the sink holds the transformed records'
        registry.getById(EtlExample.ID_TARGET).instance.sinkView() == ['ALPHA', 'BRAVO', 'CHARLIE']

        cleanup:
        registry.shutdown(true)
    }

    def 'shutdown gracefully destroys the whole started pipeline'() {
        given: 'a fully started pipeline'
        def registry = new DefaultActiveObjectsDispatcher()
        def startOrder = new CopyOnWriteArrayList<String>()
        registry.create({ id, options -> new TargetEndpoint(id, options) }, EtlExample.ID_TARGET, startOrder)
        registry.create({ id, options -> new TransformerEndpoint(id, options) }, EtlExample.ID_TRANSFORMER, startOrder)
        registry.create({ id, options -> new SourceEndpoint(id, options) }, EtlExample.ID_SOURCE, startOrder)
        registry.getById(EtlExample.ID_TARGET).request(TargetEndpoint.MSG_LOAD).get(5, TimeUnit.SECONDS)

        when: 'the dispatcher is shut down gracefully'
        registry.shutdown(false)

        then: 'every stage has been destroyed'
        registry.findById(EtlExample.ID_TARGET) == null
        registry.findById(EtlExample.ID_TRANSFORMER) == null
        registry.findById(EtlExample.ID_SOURCE) == null
    }
}
