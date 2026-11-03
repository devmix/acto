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

package com.github.devmix.process.acto.examples.misc;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

/**
 * A game player session: purchases are validated and applied in one sequential step, so
 * the player can never overspend no matter how fast the client clicks.
 *
 * @author Sergey Grachev
 */
public class PlayerSessionObject implements OnMessageListener<PlayerSessionObject> {

    public record Buy(String item, int cost) {
    }

    public record Gold() {
    }

    private int gold;

    public PlayerSessionObject(final Object id, final Integer gold) {
        this.gold = gold;
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<PlayerSessionObject> context) {
        if (message instanceof Buy buy) {
            if (buy.cost() > 0 && buy.cost() <= gold) {
                gold -= buy.cost();
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if (message instanceof Gold) {
            return gold;
        }
        return null;
    }
}
