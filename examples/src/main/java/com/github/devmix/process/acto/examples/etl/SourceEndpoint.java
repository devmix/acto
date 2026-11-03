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
import com.github.devmix.process.acto.listeners.OnMessageListener;
import com.github.devmix.process.acto.listeners.OnStartListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The first stage of the ETL pipeline: pretends to connect to an external system and
 * exposes raw records. Every pipeline stage records its start into the shared
 * {@code startOrder} list so that the example can prove the dependency-driven start order.
 *
 * @author Sergey Grachev
 */
public class SourceEndpoint implements OnMessageListener<SourceEndpoint>, OnStartListener<SourceEndpoint> {

    public static final String MSG_EXTRACT = "extract";

    private static final List<String> RAW_RECORDS = List.of("alpha", "bravo", "charlie");

    private final CopyOnWriteArrayList<String> startOrder;
    private boolean connected;

    public SourceEndpoint(final Object id, final CopyOnWriteArrayList<String> startOrder) {
        this.startOrder = startOrder;
    }

    @Override
    public void onObjectStart(final com.github.devmix.process.acto.messages.Start message,
                              final ActiveObjectContext<SourceEndpoint> context) {
        connected = true;
        startOrder.add(String.valueOf(context.getId()));
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<SourceEndpoint> context) {
        if (MSG_EXTRACT.equals(message)) {
            if (!connected) {
                throw new IllegalStateException("source is not connected");
            }
            return RAW_RECORDS;
        }
        return null;
    }
}
