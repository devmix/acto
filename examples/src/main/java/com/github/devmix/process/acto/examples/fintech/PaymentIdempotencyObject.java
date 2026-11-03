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

import java.util.HashMap;
import java.util.Map;

/**
 * A payment idempotency store: a retried request with the same idempotency key gets the
 * cached result, so a client retry storm cannot double-charge.
 *
 * @author Sergey Grachev
 */
public class PaymentIdempotencyObject implements OnMessageListener<PaymentIdempotencyObject> {

    public record Pay(String key, long amount) {
    }

    public record Charged() {
    }

    private final Map<String, String> results = new HashMap<>();
    private long charged;

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<PaymentIdempotencyObject> context) {
        if (message instanceof Pay pay) {
            final var cached = results.get(pay.key());
            if (cached != null) {
                return cached;
            }
            charged += pay.amount();
            final var result = "CHARGED:" + pay.amount();
            results.put(pay.key(), result);
            return result;
        }
        if (message instanceof Charged) {
            return charged;
        }
        return null;
    }
}
