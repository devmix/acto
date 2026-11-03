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

package com.github.devmix.process.acto

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import com.github.devmix.process.acto.listeners.OnMessageListener
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicReference

/**
 * @author Sergey Grachev
 */
class DispatchThreadNamingSpec extends Specification {

    static class NameCapture implements OnMessageListener<NameCapture> {
        final AtomicReference<String> threadName = new AtomicReference<>()

        @Override
        Object onObjectMessage(final Object message, final ActiveObjectContext<NameCapture> context) {
            threadName.set(Thread.currentThread().name)
            return message
        }
    }

    def "dispatch thread is named after the object it serves"() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def listener = new NameCapture()
        def object = registry.create({ id, options -> listener }, 'named-object', null)

        when:
        object.request('m').get(30, java.util.concurrent.TimeUnit.SECONDS)

        then:
        listener.threadName.get() == 'acto-dispatch-named-object'

        cleanup:
        registry.destroy('named-object', true)
    }
}
