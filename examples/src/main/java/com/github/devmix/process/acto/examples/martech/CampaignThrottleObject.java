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

package com.github.devmix.process.acto.examples.martech;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.HashMap;
import java.util.Map;

/**
 * A per-campaign send throttle: each campaign gets a token bucket refilled lazily on demand,
 * so no scheduler thread is needed and the buckets are consistent under any send rate.
 *
 * @author Sergey Grachev
 */
public class CampaignThrottleObject implements OnMessageListener<CampaignThrottleObject> {

    public record Send(String campaign) {
    }

    public record Sent(String campaign) {
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefill;
        private int sent;
    }

    private final Map<String, Bucket> buckets = new HashMap<>();
    private final double ratePerSecond;
    private final double capacity;

    public CampaignThrottleObject(final Object id, final Double ratePerSecond) {
        this.ratePerSecond = ratePerSecond;
        this.capacity = ratePerSecond;
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<CampaignThrottleObject> context) {
        if (message instanceof Send send) {
            final var bucket = buckets.computeIfAbsent(send.campaign(), c -> new Bucket());
            refill(bucket);
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                bucket.sent++;
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if (message instanceof Sent sent) {
            final var bucket = buckets.get(sent.campaign());
            return bucket == null ? 0 : bucket.sent;
        }
        return null;
    }

    private void refill(final Bucket bucket) {
        final var now = System.nanoTime();
        final var elapsed = (now - bucket.lastRefill) / 1_000_000_000.0;
        bucket.lastRefill = now;
        bucket.tokens = Math.min(capacity, bucket.tokens + elapsed * ratePerSecond);
    }
}
