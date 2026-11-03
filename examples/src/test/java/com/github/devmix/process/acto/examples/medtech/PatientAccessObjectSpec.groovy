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

import com.github.devmix.process.acto.core.config.DispatcherConfig
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class PatientAccessObjectSpec extends Specification {

    def 'an open record is auto-closed and audited when the clinician goes silent'() {
        given:
        def config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def record = registry.create({ id, options -> new PatientAccessObject() }, 'patient-1', null)

        when: 'a clinician opens the record and walks away'
        record.request(new PatientAccessObject.Open('dr.who')).get(5, TimeUnit.SECONDS)
        Thread.sleep(500)

        then: 'the heartbeat closed the access'
        record.<String>request(new PatientAccessObject.Access()).get(5, TimeUnit.SECONDS) == null

        and: 'the audit trail shows both events'
        def audit = record.<List<String>>request(new PatientAccessObject.Audit()).get(5, TimeUnit.SECONDS)
        audit.first() == 'OPEN:dr.who'
        audit.last() == 'AUTO_CLOSE:dr.who'

        cleanup:
        registry.shutdown(true)
    }
}
