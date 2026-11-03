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
class ChatConversationObjectSpec extends Specification {

    def 'concurrent sends get a gapless total order'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def chat = registry.create({ id, options -> new ChatConversationObject() }, 'chat', null)

        when: '50 messages are sent concurrently'
        (1..50).collect { chat.request(new ChatConversationObject.Send("u$it", "hi")) }*.get(5, TimeUnit.SECONDS)

        then: 'sequence numbers are 1..50 without gaps'
        def history = chat.<List<String>>request(new ChatConversationObject.History()).get(5, TimeUnit.SECONDS)
        history.size() == 50
        history.withIndex().every { msg, i -> msg.startsWith("${i + 1} u") }

        cleanup:
        registry.shutdown(true)
    }
}
