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

package com.github.devmix.process.acto.messages;

/**
 * Functional interface representing an operation that sets a value to a target object.
 *
 * @param <T> the type of the target object
 * @param <V> the type of the value to be set
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface AttributeSetter<T, V> {

    /**
     * Sets the specified value to the given target object.
     *
     * @param target the target object to which the value is to be set
     * @param value  the value to set on the target object
     * @throws Exception if an error occurs during setting of the value
     */
    void set(T target, V value) throws Exception;
}
