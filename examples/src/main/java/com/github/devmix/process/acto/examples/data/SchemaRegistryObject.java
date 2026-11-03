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

package com.github.devmix.process.acto.examples.data;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.HashMap;
import java.util.Map;

/**
 * A schema registry: versions per subject must advance by one, and concurrent registration
 * races are serialized by the mailbox so no two producers can claim the same version.
 *
 * @author Sergey Grachev
 */
public class SchemaRegistryObject implements OnMessageListener<SchemaRegistryObject> {

    public record Register(String subject, int version, String schema) {
    }

    public record Latest(String subject) {
    }

    private final Map<String, String> latest = new HashMap<>();
    private final Map<String, Integer> versions = new HashMap<>();

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<SchemaRegistryObject> context) {
        if (message instanceof Register register) {
            final var current = versions.getOrDefault(register.subject(), 0);
            if (register.version() != current + 1) {
                return "CONFLICT:expected " + (current + 1);
            }
            versions.put(register.subject(), register.version());
            latest.put(register.subject(), register.schema());
            return Boolean.TRUE;
        }
        if (message instanceof Latest latest) {
            return this.latest.get(latest.subject());
        }
        return null;
    }
}
