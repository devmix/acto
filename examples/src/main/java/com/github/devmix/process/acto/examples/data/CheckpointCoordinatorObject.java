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
 * A sink checkpoint coordinator: records are written through to the sink and the accepted
 * watermark only advances after the write. Graceful destroy guarantees that every accepted
 * record reached the sink before the coordinator dies — the exactly-once commit rule.
 *
 * @author Sergey Grachev
 */
public class CheckpointCoordinatorObject implements OnMessageListener<CheckpointCoordinatorObject> {

    public record Process(long offset, String record) {
    }

    public record Sink() {
    }

    public record Watermark() {
    }

    private final List<String> sink = new ArrayList<>();
    private long watermark = -1;

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<CheckpointCoordinatorObject> context) {
        if (message instanceof Process process) {
            sink.add(process.record());
            watermark = Math.max(watermark, process.offset());
            return watermark;
        }
        if (message instanceof Sink) {
            return List.copyOf(sink);
        }
        if (message instanceof Watermark) {
            return watermark;
        }
        return null;
    }
}
