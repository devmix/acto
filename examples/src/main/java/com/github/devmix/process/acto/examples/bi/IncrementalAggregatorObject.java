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
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.ArrayList;
import java.util.List;

/**
 * An incremental analytics aggregator: concurrent readers and writers share one mailbox,
 * and quantiles are computed from the exact buffer — no concurrent data structure needed.
 *
 * @author Sergey Grachev
 */
public class IncrementalAggregatorObject implements OnMessageListener<IncrementalAggregatorObject> {

    public record Add(double value) {
    }

    public record Quantile(double q) {
    }

    public record Count() {
    }

    private final List<Double> values = new ArrayList<>();

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<IncrementalAggregatorObject> context) {
        if (message instanceof Add add) {
            values.add(add.value());
            return values.size();
        }
        if (message instanceof Quantile quantile) {
            if (values.isEmpty()) {
                return null;
            }
            final var sorted = new ArrayList<>(values);
            sorted.sort(Double::compare);
            final var index = (int) Math.round(quantile.q() * (sorted.size() - 1));
            return sorted.get(index);
        }
        if (message instanceof Count) {
            return values.size();
        }
        return null;
    }
}
