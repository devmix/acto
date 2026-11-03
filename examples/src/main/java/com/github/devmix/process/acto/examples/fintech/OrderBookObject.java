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

package com.github.devmix.process.acto.examples.fintech;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A level-2 order book as a single-writer object: the book is plain {@link TreeMap} code
 * with no locks, yet it is safe under any concurrency because every mutation arrives through
 * the mailbox in a deterministic order.
 *
 * @author Sergey Grachev
 */
public class OrderBookObject implements OnMessageListener<OrderBookObject> {

    public record Add(String orderId, boolean buy, long price, int qty) {
    }

    public record Cancel(String orderId) {
    }

    public record Best() {
    }

    private final TreeMap<Long, Integer> bids = new TreeMap<>(Collections.reverseOrder());
    private final TreeMap<Long, Integer> asks = new TreeMap<>();
    private final Map<String, Order> orders = new HashMap<>();

    private record Order(boolean buy, long price, int qty) {
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<OrderBookObject> context) {
        if (message instanceof Add add) {
            final var levels = add.buy() ? bids : asks;
            levels.merge(add.price(), add.qty(), Integer::sum);
            orders.put(add.orderId(), new Order(add.buy(), add.price(), add.qty()));
            return Boolean.TRUE;
        }
        if (message instanceof Cancel cancel) {
            final var order = orders.remove(cancel.orderId());
            if (order == null) {
                return Boolean.FALSE;
            }
            final var levels = order.buy() ? bids : asks;
            levels.compute(order.price(), (price, qty) -> qty - order.qty() <= 0 ? null : qty - order.qty());
            return Boolean.TRUE;
        }
        if (message instanceof Best) {
            final var best = new HashMap<String, Long>();
            best.put("bid", bids.isEmpty() ? null : bids.firstKey());
            best.put("ask", asks.isEmpty() ? null : asks.firstKey());
            return best;
        }
        return null;
    }
}
