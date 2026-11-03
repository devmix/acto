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

package com.github.devmix.process.acto.examples.medtech

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class LabOrderObjectSpec extends Specification {

    def 'a result can only be published after the analysis has started'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def order = registry.create({ id, options -> new LabOrderObject() }, 'lab-1', null)

        when: 'the result is published too early'
        def early = order.request(new LabOrderObject.PublishResult('5.4')).get(5, TimeUnit.SECONDS)

        then: 'it is rejected'
        early.toString().startsWith('ILLEGAL')

        when: 'the order walks the proper path'
        order.request(new LabOrderObject.Collect()).get(5, TimeUnit.SECONDS)
        order.request(new LabOrderObject.StartAnalysis()).get(5, TimeUnit.SECONDS)
        order.request(new LabOrderObject.PublishResult('5.4')).get(5, TimeUnit.SECONDS)

        then:
        order.<String>request(new LabOrderObject.StateQuery()).get(5, TimeUnit.SECONDS) == 'RESULTED:5.4'

        cleanup:
        registry.shutdown(true)
    }
}
