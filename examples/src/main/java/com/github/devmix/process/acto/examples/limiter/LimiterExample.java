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

package com.github.devmix.process.acto.examples.limiter;

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher;

/**
 * Use case 4: a rate limiter as an active object — the mailbox is the mutex.
 *
 * @author Sergey Grachev
 */
public final class LimiterExample {

    private LimiterExample() {
    }

    public static void main(final String[] args) throws Exception {
        final var registry = new DefaultActiveObjectsDispatcher();
        final var limiter = registry.create(RateLimiterObject::new, "limiter", 3);

        for (var i = 1; i <= 5; i++) {
            final var acquired = limiter.<Boolean>request(new RateLimiterObject.TryAcquire()).get();
            System.out.println("attempt " + i + ": acquired=" + acquired
                    + ", available=" + limiter.<Integer>request(new RateLimiterObject.Available()).get());
        }

        limiter.request(new RateLimiterObject.Release()).get();
        System.out.println("after release: acquired="
                + limiter.<Boolean>request(new RateLimiterObject.TryAcquire()).get());

        registry.shutdown(true);
    }
}
