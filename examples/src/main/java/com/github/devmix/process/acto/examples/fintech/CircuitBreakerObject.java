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

/**
 * A kill switch / circuit breaker: the tripped flag is flipped by a single message, so a
 * "stop the world" command is ordered with respect to every in-flight request — no request
 * slips through after the breaker trips.
 *
 * @author Sergey Grachev
 */
public class CircuitBreakerObject implements OnMessageListener<CircuitBreakerObject> {

    public record Request() {
    }

    public record Trip() {
    }

    public record Reset() {
    }

    public record Tripped() {
    }

    private boolean tripped;

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<CircuitBreakerObject> context) {
        if (message instanceof Request) {
            return tripped ? "REJECTED" : "OK";
        }
        if (message instanceof Trip) {
            tripped = true;
            return null;
        }
        if (message instanceof Reset) {
            tripped = false;
            return null;
        }
        if (message instanceof Tripped) {
            return tripped;
        }
        return null;
    }
}
