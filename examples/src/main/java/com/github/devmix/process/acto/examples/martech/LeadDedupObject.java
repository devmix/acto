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

package com.github.devmix.process.acto.examples.martech;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.HashMap;
import java.util.Map;

/**
 * A lead deduplication gateway: a lead is forwarded only once per TTL window. The window
 * is enforced lazily on arrival, so duplicate leads arriving from many channels at once
 * collapse deterministically.
 *
 * @author Sergey Grachev
 */
public class LeadDedupObject implements OnMessageListener<LeadDedupObject> {

    public record Ingest(String leadId, String channel) {
    }

    public record Forwarded() {
    }

    private final Map<String, Long> seen = new HashMap<>();
    private final long ttlMillis;
    private int forwarded;

    public LeadDedupObject(final Object id, final Long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<LeadDedupObject> context) {
        if (message instanceof Ingest ingest) {
            final var now = System.currentTimeMillis();
            final var last = seen.get(ingest.leadId());
            if (last != null && now - last < ttlMillis) {
                return Boolean.FALSE;
            }
            seen.put(ingest.leadId(), now);
            forwarded++;
            return Boolean.TRUE;
        }
        if (message instanceof Forwarded) {
            return forwarded;
        }
        return null;
    }
}
