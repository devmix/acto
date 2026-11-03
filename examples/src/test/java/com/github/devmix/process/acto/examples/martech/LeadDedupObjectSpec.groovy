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
class LeadDedupObjectSpec extends Specification {

    def 'the same lead is forwarded once per TTL window'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def dedup = registry.create({ id, options -> new LeadDedupObject(id, options) }, 'dedup', 200L)

        when: 'the same lead arrives from three channels at once'
        def results = ['web', 'email', 'phone'].collect {
            dedup.request(new LeadDedupObject.Ingest('lead-1', it))
        }*.get(5, TimeUnit.SECONDS)

        then: 'only the first is forwarded'
        results.count { it == Boolean.TRUE } == 1
        dedup.<Integer>request(new LeadDedupObject.Forwarded()).get(5, TimeUnit.SECONDS) == 1

        when: 'the TTL window passes and the lead arrives again'
        Thread.sleep(300)

        then:
        dedup.<Boolean>request(new LeadDedupObject.Ingest('lead-1', 'web')).get(5, TimeUnit.SECONDS) == Boolean.TRUE

        cleanup:
        registry.shutdown(true)
    }
}
