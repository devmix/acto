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

package com.github.devmix.process.acto.examples.cache;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.messages.Idle;
import com.github.devmix.process.acto.listeners.OnIdleListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A write-through cache: concurrent readers and writers funnel through one mailbox, every
 * write is persisted to the backing store synchronously inside the object, and the in-memory
 * map is dropped when the heartbeat deactivates the object after the idle timeout.
 *
 * @author Sergey Grachev
 */
public class WriteThroughCacheObject implements OnMessageListener<WriteThroughCacheObject>,
        OnIdleListener<WriteThroughCacheObject> {

    public record Put(String key, String value) {
    }

    public record Get(String key) {
    }

    public record Persisted() {
    }

    public record Deactivations() {
    }

    private final Map<String, String> cache = new HashMap<>();
    private final List<String> store = new ArrayList<>();
    private int deactivations;

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<WriteThroughCacheObject> context) {
        if (message instanceof Put put) {
            cache.put(put.key(), put.value());
            store.add(put.key() + "=" + put.value());
            return Boolean.TRUE;
        }
        if (message instanceof Get get) {
            return cache.get(get.key());
        }
        if (message instanceof Persisted) {
            return List.copyOf(store);
        }
        if (message instanceof Deactivations) {
            return deactivations;
        }
        return null;
    }

    @Override
    public boolean onObjectActivate(final Idle.Activate message, final ActiveObjectContext<WriteThroughCacheObject> context) {
        return true;
    }

    @Override
    public boolean onObjectDeactivate(final Idle.Deactivate message, final ActiveObjectContext<WriteThroughCacheObject> context) {
        cache.clear();
        deactivations++;
        return true;
    }

    public List<String> storeView() {
        return Collections.unmodifiableList(store);
    }
}
