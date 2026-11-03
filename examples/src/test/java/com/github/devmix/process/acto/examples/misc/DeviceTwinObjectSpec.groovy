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

import com.github.devmix.process.acto.core.config.DispatcherConfig
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class DeviceTwinObjectSpec extends Specification {

    def 'the twin merges reports and goes offline when devices stop reporting'() {
        given:
        def config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def twin = registry.create({ id, options -> new DeviceTwinObject() }, 'twin', null)

        when: 'a device reports two attributes'
        twin.request(new DeviceTwinObject.Report('lock-1', 'mode', 'auto')).get(5, TimeUnit.SECONDS)
        twin.request(new DeviceTwinObject.Report('lock-1', 'battery', '87')).get(5, TimeUnit.SECONDS)

        then: 'the twin holds the merged state'
        twin.<Map<String, String>>request(new DeviceTwinObject.Get('lock-1')).get(5, TimeUnit.SECONDS) == [mode: 'auto', battery: '87']

        when: 'the devices stop reporting'
        Thread.sleep(500)

        then: 'the heartbeat marked the twin offline'
        twin.<Boolean>request(new DeviceTwinObject.Online()).get(5, TimeUnit.SECONDS) == Boolean.FALSE

        cleanup:
        registry.shutdown(true)
    }
}
