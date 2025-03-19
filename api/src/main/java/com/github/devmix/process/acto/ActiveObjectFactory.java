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

package com.github.devmix.process.acto;

import javax.annotation.Nullable;

/**
 * Factory interface for creating ActiveObject instances.
 *
 * <p>This interface defines a method to create an instance of type {@code T}
 * using an identifier of type {@code I} and optional options of type {@code O}.
 *
 * @param <T> the type of ActiveObject to be created
 * @param <I> the type of the identifier used for creating the ActiveObject
 * @param <O> the type of the optional options used during creation, can be null
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface ActiveObjectFactory<T, I, O> {

    /**
     * Creates an instance of an ActiveObject.
     *
     * @param id      the identifier for the ActiveObject to be created
     * @param options optional configuration options; may be null if not required
     * @return a new instance of the ActiveObject type {@code T}
     */
    T create(I id, @Nullable O options);
}
