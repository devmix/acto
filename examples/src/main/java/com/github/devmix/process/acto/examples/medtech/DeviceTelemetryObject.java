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

package com.github.devmix.process.acto.examples.medtech;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.messages.Idle;
import com.github.devmix.process.acto.listeners.OnIdleListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.HashMap;
import java.util.Map;

/**
 * A medical device telemetry aggregator: the latest reading per device is kept in memory,
 * and when the heartbeat deactivates the object the devices are marked offline — the
 * "device went silent" detection costs no polling thread.
 *
 * @author Sergey Grachev
 */
public class DeviceTelemetryObject implements OnMessageListener<DeviceTelemetryObject>, OnIdleListener<DeviceTelemetryObject> {

    public record Reading(String device, int value) {
    }

    public record Latest(String device) {
    }

    public record Online() {
    }

    private final Map<String, Integer> latest = new HashMap<>();
    private boolean online;

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<DeviceTelemetryObject> context) {
        if (message instanceof Reading reading) {
            latest.put(reading.device(), reading.value());
            online = true;
            return latest.size();
        }
        if (message instanceof Latest latest) {
            return this.latest.get(latest.device());
        }
        if (message instanceof Online) {
            return online;
        }
        return null;
    }

    @Override
    public boolean onObjectActivate(final Idle.Activate message, final ActiveObjectContext<DeviceTelemetryObject> context) {
        return true;
    }

    @Override
    public boolean onObjectDeactivate(final Idle.Deactivate message, final ActiveObjectContext<DeviceTelemetryObject> context) {
        online = false;
        return true;
    }
}
