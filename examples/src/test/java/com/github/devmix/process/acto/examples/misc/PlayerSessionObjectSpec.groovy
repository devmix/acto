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
class PlayerSessionObjectSpec extends Specification {

    def 'a click storm can never overspend the player'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def player = registry.create({ id, options -> new PlayerSessionObject(id, options) }, 'player', 100)

        when: '200 buy clicks of 10 gold race'
        def results = (1..200).collect { player.request(new PlayerSessionObject.Buy('sword', 10)) }
                *.get(5, TimeUnit.SECONDS)

        then: 'exactly 10 purchases succeed and the gold is zero'
        results.count { it == Boolean.TRUE } == 10
        player.<Integer>request(new PlayerSessionObject.Gold()).get(5, TimeUnit.SECONDS) == 0

        cleanup:
        registry.shutdown(true)
    }
}
