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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Exactly-once event application: a market-data feed that redelivers events is deduplicated
 * by sequence number inside the object, so the applied view is correct no matter how many
 * duplicates or racing producers there are.
 *
 * @author Sergey Grachev
 */
public class IdempotentApplierObject implements OnMessageListener<IdempotentApplierObject> {

    public record Apply(long seq, String value) {
    }

    public record Applied() {
    }

    public record Duplicates() {
    }

    private final Set<Long> seen = new HashSet<>();
    private final List<String> applied = new ArrayList<>();
    private int duplicates;

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<IdempotentApplierObject> context) {
        if (message instanceof Apply apply) {
            if (seen.add(apply.seq())) {
                applied.add(apply.value());
                return Boolean.TRUE;
            }
            duplicates++;
            return Boolean.FALSE;
        }
        if (message instanceof Applied) {
            return List.copyOf(applied);
        }
        if (message instanceof Duplicates) {
            return duplicates;
        }
        return null;
    }
}
