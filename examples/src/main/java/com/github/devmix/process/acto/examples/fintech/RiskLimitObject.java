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
 * Pre-trade risk check: the exposure update and the limit check happen in one sequential
 * step inside the mailbox, so the limit can never be breached by racing trades.
 *
 * @author Sergey Grachev
 */
public class RiskLimitObject implements OnMessageListener<RiskLimitObject> {

    public record Trade(long notional) {
    }

    public record Exposure() {
    }

    private final long maxExposure;
    private long exposure;

    public RiskLimitObject(final Object id, final Long maxExposure) {
        this.maxExposure = maxExposure;
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<RiskLimitObject> context) {
        if (message instanceof Trade trade) {
            final var next = exposure + trade.notional();
            if (Math.abs(next) > maxExposure) {
                return Boolean.FALSE;
            }
            exposure = next;
            return Boolean.TRUE;
        }
        if (message instanceof Exposure) {
            return exposure;
        }
        return null;
    }
}
