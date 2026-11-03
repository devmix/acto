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

package com.github.devmix.process.acto.examples.martech

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class CampaignThrottleObjectSpec extends Specification {

    def 'a burst is capped by the bucket and refills over time'() {
        given: 'a campaign throttled to 5 sends per second'
        def registry = new DefaultActiveObjectsDispatcher()
        def throttle = registry.create({ id, options -> new CampaignThrottleObject(id, options) }, 'throttle', 5.0d)

        when: '10 sends hit at once'
        def results = (1..10).collect { throttle.request(new CampaignThrottleObject.Send('black-friday')) }
                *.get(5, TimeUnit.SECONDS)

        then: 'the bucket capacity caps the burst'
        results.count { it == Boolean.TRUE } == 5

        when: 'a second passes'
        Thread.sleep(1100)

        then: 'the bucket refilled and sends flow again'
        throttle.<Boolean>request(new CampaignThrottleObject.Send('black-friday')).get(5, TimeUnit.SECONDS) == Boolean.TRUE

        cleanup:
        registry.shutdown(true)
    }
}
