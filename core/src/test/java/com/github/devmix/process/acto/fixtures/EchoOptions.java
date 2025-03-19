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

package com.github.devmix.process.acto.fixtures;

import com.github.devmix.process.acto.core.context.DefaultActiveObjectContext;
import lombok.Getter;

import java.util.List;

/**
 * @author Sergey Grachev
 */
@Getter
public final class EchoOptions {

    private final List<String> dependsOn;
    private final List<String> dependsOnOptional;
    private final boolean allowIdle;
    private final long idleTimeout;

    public EchoOptions(final boolean allowIdle, final long idleTimeout, final List<String> dependsOn, final List<String> dependsOnOptional) {
        this.idleTimeout = idleTimeout;
        this.dependsOn = dependsOn;
        this.dependsOnOptional = dependsOnOptional;
        this.allowIdle = allowIdle;
    }

    public EchoOptions(final List<String> dependsOn, final List<String> dependsOnOptional) {
        this(false, DefaultActiveObjectContext.NO_DEACTIVATION_TIMEOUT, dependsOn, dependsOnOptional);
    }

    public EchoOptions(final boolean allowIdle, final long idleTimeout) {
        this(allowIdle, idleTimeout, null, null);
    }
}
