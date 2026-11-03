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

package com.github.devmix.process.acto.examples.logistics;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.HashMap;
import java.util.Map;

/**
 * A vehicle pool with lease expiry: leases are reclaimed lazily on every borrow/return,
 * so no timer thread is needed — the mailbox traffic drives the cleanup.
 *
 * @author Sergey Grachev
 */
public class VehiclePoolObject implements OnMessageListener<VehiclePoolObject> {

    public record Borrow(String customer) {
    }

    public record Return(String vehicle) {
    }

    public record Free() {
    }

    private final Map<String, Long> leases = new HashMap<>();
    private final long leaseMillis;
    private int nextVehicle = 1;

    public VehiclePoolObject(final Object id, final Long leaseMillis) {
        this.leaseMillis = leaseMillis;
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<VehiclePoolObject> context) {
        if (message instanceof Borrow borrow) {
            sweep();
            final var vehicle = "van-" + nextVehicle++;
            leases.put(vehicle, System.currentTimeMillis() + leaseMillis);
            return vehicle;
        }
        if (message instanceof Return ret) {
            sweep();
            return leases.remove(ret.vehicle()) != null;
        }
        if (message instanceof Free) {
            sweep();
            return leases.size();
        }
        return null;
    }

    private void sweep() {
        final var now = System.currentTimeMillis();
        leases.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
