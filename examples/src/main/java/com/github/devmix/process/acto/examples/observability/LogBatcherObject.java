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

package com.github.devmix.process.acto.examples.observability;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.messages.Idle;
import com.github.devmix.process.acto.listeners.OnIdleListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A log/metric batcher: lines are buffered and flushed when the batch is full or when the
 * heartbeat deactivates the object — the idle timeout is the "flush after quiet period"
 * timer, so no scheduler thread exists.
 *
 * @author Sergey Grachev
 */
public class LogBatcherObject implements OnMessageListener<LogBatcherObject>, OnIdleListener<LogBatcherObject> {

    public record Log(String line) {
    }

    public record Batches() {
    }

    public record Pending() {
    }

    private final int batchSize;
    private final List<String> buffer = new ArrayList<>();
    private final List<List<String>> batches = new ArrayList<>();

    public LogBatcherObject(final Object id, final Integer batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<LogBatcherObject> context) {
        if (message instanceof Log log) {
            buffer.add(log.line());
            if (buffer.size() >= batchSize) {
                flush();
            }
            return batches.size();
        }
        if (message instanceof Batches) {
            return List.copyOf(batches);
        }
        if (message instanceof Pending) {
            return buffer.size();
        }
        return null;
    }

    @Override
    public boolean onObjectActivate(final Idle.Activate message, final ActiveObjectContext<LogBatcherObject> context) {
        return true;
    }

    @Override
    public boolean onObjectDeactivate(final Idle.Deactivate message, final ActiveObjectContext<LogBatcherObject> context) {
        flush();
        return true;
    }

    private void flush() {
        if (!buffer.isEmpty()) {
            batches.add(List.copyOf(buffer));
            buffer.clear();
        }
    }
}
