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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Shipment tracking with out-of-order events: events are accepted in any arrival order but
 * applied strictly in sequence order — late events are buffered until their turn. The
 * buffering is trivial sequential code because the object owns the state.
 *
 * @author Sergey Grachev
 */
public class ShipmentTrackerObject implements OnMessageListener<ShipmentTrackerObject> {

    public record Event(long seq, String state) {
    }

    public record History() {
    }

    public record Buffered() {
    }

    private final TreeMap<Long, String> pending = new TreeMap<>();
    private final List<String> history = new ArrayList<>();
    private long nextSeq = 1;

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<ShipmentTrackerObject> context) {
        if (message instanceof Event event) {
            if (event.seq() >= nextSeq) {
                pending.put(event.seq(), event.state());
            }
            while (pending.containsKey(nextSeq)) {
                history.add(pending.remove(nextSeq++));
            }
            return history.size();
        }
        if (message instanceof History) {
            return List.copyOf(history);
        }
        if (message instanceof Buffered) {
            return pending.size();
        }
        return null;
    }
}
