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

package com.github.devmix.process.acto.examples.edtech

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class ProgressJournalObjectSpec extends Specification {

    def 'concurrent events are journaled and fully replayable'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def journal = registry.create({ id, options -> new ProgressJournalObject() }, 'journal', null)

        when: '200 events arrive concurrently'
        (1..200).collect { journal.request(new ProgressJournalObject.Record("e$it")) }*.get(5, TimeUnit.SECONDS)

        then: 'the replay contains every event exactly once'
        def replay = journal.<List<String>>request(new ProgressJournalObject.Replay()).get(5, TimeUnit.SECONDS)
        replay.size() == 200
        replay.toSet().size() == 200

        cleanup:
        registry.shutdown(true)
    }
}
