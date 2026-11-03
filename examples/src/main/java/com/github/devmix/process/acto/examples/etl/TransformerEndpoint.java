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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The middle stage: declares {@code DEPENDS_ON} the source, so the dispatcher guarantees
 * that the source is started (connected) before this object processes anything.
 *
 * @author Sergey Grachev
 */
public class TransformerEndpoint implements OnCreateListener<TransformerEndpoint>,
        OnStartListener<TransformerEndpoint>, OnMessageListener<TransformerEndpoint> {

    public static final String MSG_TRANSFORM = "transform";

    private final CopyOnWriteArrayList<String> startOrder;

    public TransformerEndpoint(final Object id, final CopyOnWriteArrayList<String> startOrder) {
        this.startOrder = startOrder;
    }

    @Override
    public void onObjectCreate(final ActiveObjectContext<TransformerEndpoint> context) {
        context.addDependsOn(EtlExample.ID_SOURCE, false);
    }

    @Override
    public void onObjectStart(final com.github.devmix.process.acto.messages.Start message,
                              final ActiveObjectContext<TransformerEndpoint> context) {
        startOrder.add(String.valueOf(context.getId()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object onObjectMessage(final Object message, final ActiveObjectContext<TransformerEndpoint> context) {
        if (MSG_TRANSFORM.equals(message)) {
            final var raw = (List<String>) context.<List<String>>redirectRequest(EtlExample.ID_SOURCE, SourceEndpoint.MSG_EXTRACT).join();
            return raw.stream().map(String::toUpperCase).toList();
        }
        return null;
    }
}
