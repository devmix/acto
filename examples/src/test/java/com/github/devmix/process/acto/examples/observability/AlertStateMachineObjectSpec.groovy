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
class AlertStateMachineObjectSpec extends Specification {

    def 'an alert fires only after it stays pending for the idle window'() {
        given:
        def config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def alert = registry.create({ id, options -> new AlertStateMachineObject() }, 'alert', null)

        when: 'the alert resolves before the window passes'
        alert.request(new AlertStateMachineObject.Trigger()).get(5, TimeUnit.SECONDS)
        alert.request(new AlertStateMachineObject.Resolve()).get(5, TimeUnit.SECONDS)
        Thread.sleep(500)

        then: 'it never fired'
        alert.<String>request(new AlertStateMachineObject.State()).get(5, TimeUnit.SECONDS) == 'OK'

        when: 'the alert stays pending'
        alert.request(new AlertStateMachineObject.Trigger()).get(5, TimeUnit.SECONDS)
        Thread.sleep(500)

        then: 'the heartbeat promoted it to FIRING'
        alert.<String>request(new AlertStateMachineObject.State()).get(5, TimeUnit.SECONDS) == 'FIRING'

        cleanup:
        registry.shutdown(true)
    }
}
