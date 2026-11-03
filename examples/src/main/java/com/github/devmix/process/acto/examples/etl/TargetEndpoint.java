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

package com.github.devmix.process.acto.examples.etl;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnCreateListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;
import com.github.devmix.process.acto.listeners.OnStartListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The last stage: depends on the transformer. A single {@code load} message to this
 * object starts the whole pipeline (source -&gt; transformer -&gt; target) in dependency
 * order and moves the data end-to-end.
 *
 * @author Sergey Grachev
 */
public class TargetEndpoint implements OnCreateListener<TargetEndpoint>,
        OnStartListener<TargetEndpoint>, OnMessageListener<TargetEndpoint> {

    public static final String MSG_LOAD = "load";

    private final CopyOnWriteArrayList<String> startOrder;
    private final List<String> sink = new ArrayList<>();

    public TargetEndpoint(final Object id, final CopyOnWriteArrayList<String> startOrder) {
        this.startOrder = startOrder;
    }

    @Override
    public void onObjectCreate(final ActiveObjectContext<TargetEndpoint> context) {
        context.addDependsOn(EtlExample.ID_TRANSFORMER, false);
    }

    @Override
    public void onObjectStart(final com.github.devmix.process.acto.messages.Start message,
                              final ActiveObjectContext<TargetEndpoint> context) {
        startOrder.add(String.valueOf(context.getId()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object onObjectMessage(final Object message, final ActiveObjectContext<TargetEndpoint> context) {
        if (MSG_LOAD.equals(message)) {
            final var transformed = (List<String>) context.<List<String>>redirectRequest(EtlExample.ID_TRANSFORMER, TransformerEndpoint.MSG_TRANSFORM).join();
            sink.addAll(transformed);
            return sink.size();
        }
        return null;
    }

    public List<String> sinkView() {
        return Collections.unmodifiableList(sink);
    }
}
