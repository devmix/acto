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

package com.github.devmix.process.acto.examples.erp;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

/**
 * Warehouse stock reservation: reserve/release/commit as sequential code, so overselling
 * is impossible without a single CAS loop or lock.
 *
 * @author Sergey Grachev
 */
public class InventoryReservationObject implements OnMessageListener<InventoryReservationObject> {

    public record Reserve(int qty) {
    }

    public record Release(int qty) {
    }

    public record Available() {
    }

    private int available;

    public InventoryReservationObject(final Object id, final Integer stock) {
        this.available = stock;
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<InventoryReservationObject> context) {
        if (message instanceof Reserve reserve) {
            if (reserve.qty() > 0 && reserve.qty() <= available) {
                available -= reserve.qty();
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if (message instanceof Release release) {
            available = Math.max(0, available + release.qty());
            return null;
        }
        if (message instanceof Available) {
            return available;
        }
        return null;
    }
}
