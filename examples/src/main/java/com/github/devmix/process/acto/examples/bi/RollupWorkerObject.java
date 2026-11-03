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

package com.github.devmix.process.acto.examples.bi;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.messages.Idle;
import com.github.devmix.process.acto.listeners.OnIdleListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A metrics rollup worker: values are accumulated in the current bucket and the bucket is
 * emitted when the stream goes quiet — downsampling driven by the heartbeat.
 *
 * @author Sergey Grachev
 */
public class RollupWorkerObject implements OnMessageListener<RollupWorkerObject>, OnIdleListener<RollupWorkerObject> {

    public record Ingest(String metric, double value) {
    }

    public record Buckets() {
    }

    private final List<String> buckets = new ArrayList<>();
    private double sum;
    private int count;

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<RollupWorkerObject> context) {
        if (message instanceof Ingest ingest) {
            sum += ingest.value();
            count++;
            return count;
        }
        if (message instanceof Buckets) {
            return List.copyOf(buckets);
        }
        return null;
    }

    @Override
    public boolean onObjectActivate(final Idle.Activate message, final ActiveObjectContext<RollupWorkerObject> context) {
        return true;
    }

    @Override
    public boolean onObjectDeactivate(final Idle.Deactivate message, final ActiveObjectContext<RollupWorkerObject> context) {
        if (count > 0) {
            buckets.add("sum=" + sum + ",count=" + count);
            sum = 0;
            count = 0;
        }
        return true;
    }
}
