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

package com.github.devmix.process.acto.core.context;

import com.github.devmix.process.acto.ActiveObject;
import lombok.Getter;

/**
 * @author Sergey Grachev
 */
final class InternalUpstreamDependencyStopped {

    @Getter
    private final ActiveObject<?> source;

    private InternalUpstreamDependencyStopped(final ActiveObject<?> source) {
        this.source = source;
    }

    public static InternalUpstreamDependencyStopped source(final ActiveObject<?> object) {
        return new InternalUpstreamDependencyStopped(object);
    }
}
