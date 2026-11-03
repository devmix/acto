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

package com.github.devmix.process.acto.examples.data;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A dead-letter queue with linear backoff: due items are retried lazily whenever the queue
 * is touched, so retry timing needs no scheduler.
 *
 * @author Sergey Grachev
 */
public class DlqRetryObject implements OnMessageListener<DlqRetryObject> {

    public record Enqueue(String id, String payload) {
    }

    public record Flush() {
    }

    public record Pending() {
    }

    private static final class Item {
        private final String id;
        private final String payload;
        private int attempts;
        private long dueAt;

        private Item(final String id, final String payload) {
            this.id = id;
            this.payload = payload;
        }
    }

    private final long backoffMillis;
    private final List<Item> queue = new ArrayList<>();
    private final List<String> processed = new ArrayList<>();

    public DlqRetryObject(final Object id, final Long backoffMillis) {
        this.backoffMillis = backoffMillis;
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<DlqRetryObject> context) {
        if (message instanceof Enqueue enqueue) {
            final var item = new Item(enqueue.id(), enqueue.payload());
            item.dueAt = System.currentTimeMillis() + backoffMillis;
            queue.add(item);
            return queue.size();
        }
        if (message instanceof Flush) {
            final var now = System.currentTimeMillis();
            final var due = queue.stream().filter(item -> item.dueAt <= now).toList();
            for (final var item : due) {
                queue.remove(item);
                processed.add(item.id);
            }
            return due.size();
        }
        if (message instanceof Pending) {
            return queue.size();
        }
        return null;
    }

    public List<String> processedView() {
        return List.copyOf(processed);
    }
}
