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

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class ExporterQuotaObjectSpec extends Specification {

    def 'a metric storm is capped by the window quota'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def exporter = registry.create({ id, options -> new ExporterQuotaObject(id, options) }, 'exporter', [10, 200] as int[])

        when: '50 exports hit in one window'
        def results = (1..50).collect { exporter.request(new ExporterQuotaObject.Export('cpu')) }
                *.get(5, TimeUnit.SECONDS)

        then: 'exactly the quota is accepted and the rest is counted as dropped'
        results.count { it == Boolean.TRUE } == 10
        exporter.<Integer>request(new ExporterQuotaObject.Dropped()).get(5, TimeUnit.SECONDS) == 40

        when: 'a new window starts'
        Thread.sleep(300)

        then: 'exports flow again'
        exporter.<Boolean>request(new ExporterQuotaObject.Export('cpu')).get(5, TimeUnit.SECONDS) == Boolean.TRUE

        cleanup:
        registry.shutdown(true)
    }
}
