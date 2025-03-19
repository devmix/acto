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
import com.github.devmix.process.acto.listeners.OnCreateListener
import com.github.devmix.process.acto.listeners.OnDestroyListener
import com.github.devmix.process.acto.listeners.OnIdleListener
import com.github.devmix.process.acto.listeners.OnMessageListener
import com.github.devmix.process.acto.listeners.OnStartListener
import com.github.devmix.process.acto.listeners.OnStopListener
import com.github.devmix.process.acto.messages.Idle
import com.github.devmix.process.acto.messages.Start
import com.github.devmix.process.acto.messages.Stop
import spock.lang.Specification


/**
 * @author Sergey Grachev
 */
class ActiveObjectLifecycleTest extends Specification {

    def 'use external lifecycle for Map object'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        registry.registerFactory(Map.class, (id, options) -> [(id): options])
        registry.registerLifecycle(Map.class, new MapLifecycle())

        when:
        def object = registry.create(Map.class, 'map-1', 'options-1')
        then:
        object.instance == [
                'map-1' : 'options-1',
                'create': true,
        ]

        when:
        object.request('ping').get() == 'pong';
        registry.destroy(object.id, true)
        then:
        object.instance == [
                'map-1'     : 'options-1',
                'create'    : true,
                'start'     : true,
                'activate'  : true,
                'message'   : true,
                'deactivate': true,
                'stop'      : true,
                'destroy'   : true,
        ]

        cleanup:
        registry.shutdown(true)
    }

    def 'without factory'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        registry.registerLifecycle(Map.class, new MapLifecycle())

        when:
        def object = registry.create(Map.class, 'map-1', 'options-1')
        then:
        object.instance == [
                'map-1'     : 'options-1',
                'create-new': true,
        ]

        when:
        object.request('ping').get() == 'pong';
        registry.destroy(object.id, true)
        then:
        object.instance == [
                'map-1'     : 'options-1',
                'create-new': true,
                'start'     : true,
                'activate'  : true,
                'message'   : true,
                'deactivate': true,
                'stop'      : true,
                'destroy'   : true,
        ]

        cleanup:
        registry.shutdown(true)
    }

    private static final class MapLifecycle implements ActiveObjectLifecycle<Map>,
            OnCreateListener<Map>, OnStartListener<Map>, OnMessageListener<Map>, OnStopListener<Map>, OnDestroyListener<Map>, OnIdleListener<Map> {

        @Override
        void onObjectCreate(ActiveObjectContext<Map> context) {
            if (context.instance == null) {
                context.instance = [(context.id): context.options, 'create-new': true]
            } else {
                context.instance.put('create', true)
            }
        }

        @Override
        void onObjectStart(Start message, ActiveObjectContext<Map> context) {
            context.instance.put('start', true)
        }

        @Override
        Object onObjectMessage(Object message, ActiveObjectContext<Map> context) {
            context.instance.put('message', true)
            return 'ping' == message ? 'pong' : null;
        }

        @Override
        void onObjectStop(Stop message, ActiveObjectContext<Map> context) {
            context.instance.put('stop', true)
        }

        @Override
        void onObjectDestroy(ActiveObjectContext<Map> context) {
            context.instance.put('destroy', true)
        }

        @Override
        boolean onObjectActivate(Idle.Activate message, ActiveObjectContext<Map> context) {
            context.instance.put('activate', true)
            return true
        }

        @Override
        boolean onObjectDeactivate(Idle.Deactivate message, ActiveObjectContext<Map> context) {
            context.instance.put('deactivate', true)
            return true
        }
    }
}
