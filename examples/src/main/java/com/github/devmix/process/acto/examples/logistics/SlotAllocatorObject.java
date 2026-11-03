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
 * Delivery slot allocation: per-slot capacity counters, safe under concurrent booking
 * without any locking.
 *
 * @author Sergey Grachev
 */
public class SlotAllocatorObject implements OnMessageListener<SlotAllocatorObject> {

    public record Book(String slot, String customer) {
    }

    public record Cancel(String slot, String customer) {
    }

    public record Taken(String slot) {
    }

    private final int capacity;
    private final Map<String, Integer> slots = new HashMap<>();

    public SlotAllocatorObject(final Object id, final Integer capacity) {
        this.capacity = capacity;
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<SlotAllocatorObject> context) {
        if (message instanceof Book book) {
            final var taken = slots.getOrDefault(book.slot(), 0);
            if (taken < capacity) {
                slots.put(book.slot(), taken + 1);
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if (message instanceof Cancel cancel) {
            slots.computeIfPresent(cancel.slot(), (slot, taken) -> taken - 1);
            return null;
        }
        if (message instanceof Taken taken) {
            return slots.getOrDefault(taken.slot(), 0);
        }
        return null;
    }
}
