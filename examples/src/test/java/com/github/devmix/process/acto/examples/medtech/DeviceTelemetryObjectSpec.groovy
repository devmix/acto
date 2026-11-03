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
class DeviceTelemetryObjectSpec extends Specification {

    def 'silent devices are marked offline by the heartbeat'() {
        given:
        def config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def telemetry = registry.create({ id, options -> new DeviceTelemetryObject() }, 'telemetry', null)

        when: 'devices report readings'
        telemetry.request(new DeviceTelemetryObject.Reading('ventilator-1', 42)).get(5, TimeUnit.SECONDS)
        telemetry.request(new DeviceTelemetryObject.Reading('monitor-2', 7)).get(5, TimeUnit.SECONDS)

        then:
        telemetry.<Boolean>request(new DeviceTelemetryObject.Online()).get(5, TimeUnit.SECONDS) == Boolean.TRUE
        telemetry.<Integer>request(new DeviceTelemetryObject.Latest('ventilator-1')).get(5, TimeUnit.SECONDS) == 42

        when: 'the devices go silent'
        Thread.sleep(500)

        then: 'the heartbeat marked them offline'
        telemetry.<Boolean>request(new DeviceTelemetryObject.Online()).get(5, TimeUnit.SECONDS) == Boolean.FALSE

        cleanup:
        registry.shutdown(true)
    }
}
