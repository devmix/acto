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

package com.github.devmix.process.acto.examples.misc;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.messages.Idle;
import com.github.devmix.process.acto.listeners.OnIdleListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.HashMap;
import java.util.Map;

/**
 * An IoT device twin: reported state is merged per device, and the heartbeat marks the
 * twin offline when reports stop — the "last will" of the device fleet for free.
 *
 * @author Sergey Grachev
 */
public class DeviceTwinObject implements OnMessageListener<DeviceTwinObject>, OnIdleListener<DeviceTwinObject> {

    public record Report(String device, String attribute, String value) {
    }

    public record Get(String device) {
    }

    public record Online() {
    }

    private final Map<String, Map<String, String>> twins = new HashMap<>();
    private boolean online;

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<DeviceTwinObject> context) {
        if (message instanceof Report report) {
            twins.computeIfAbsent(report.device(), d -> new HashMap<>()).put(report.attribute(), report.value());
            online = true;
            return Boolean.TRUE;
        }
        if (message instanceof Get get) {
            return twins.get(get.device());
        }
        if (message instanceof Online) {
            return online;
        }
        return null;
    }

    @Override
    public boolean onObjectActivate(final Idle.Activate message, final ActiveObjectContext<DeviceTwinObject> context) {
        return true;
    }

    @Override
    public boolean onObjectDeactivate(final Idle.Deactivate message, final ActiveObjectContext<DeviceTwinObject> context) {
        online = false;
        return true;
    }
}
