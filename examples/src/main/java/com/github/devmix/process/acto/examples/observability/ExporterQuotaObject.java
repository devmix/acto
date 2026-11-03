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
import com.github.devmix.process.acto.listeners.OnMessageListener;

/**
 * A metrics exporter guard: exports are accepted up to a per-window quota and the rest are
 * dropped with a visible counter, so a metric storm cannot take down the backend.
 *
 * @author Sergey Grachev
 */
public class ExporterQuotaObject implements OnMessageListener<ExporterQuotaObject> {

    public record Export(String metric) {
    }

    public record Dropped() {
    }

    private final int quotaPerWindow;
    private final long windowMillis;
    private long windowStart = System.currentTimeMillis();
    private int accepted;
    private int dropped;

    public ExporterQuotaObject(final Object id, final int[] config) {
        this.quotaPerWindow = config[0];
        this.windowMillis = config[1];
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<ExporterQuotaObject> context) {
        if (message instanceof Export) {
            final var now = System.currentTimeMillis();
            if (now - windowStart >= windowMillis) {
                windowStart = now;
                accepted = 0;
            }
            if (accepted < quotaPerWindow) {
                accepted++;
                return Boolean.TRUE;
            }
            dropped++;
            return Boolean.FALSE;
        }
        if (message instanceof Dropped) {
            return dropped;
        }
        return null;
    }
}
