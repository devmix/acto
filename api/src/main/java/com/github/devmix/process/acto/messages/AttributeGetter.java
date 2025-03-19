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
 * Represents a function that extracts an attribute from a target object.
 *
 * <p>This functional interface is designed to be used in contexts where an attribute or property
 * of an object needs to be retrieved, potentially throwing an exception if the operation fails.</p>
 *
 * @param <T> The type of the input object (the target from which the attribute will be extracted).
 * @param <R> The type of the result (the attribute being extracted).
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface AttributeGetter<T, R> {

    /**
     * Extracts an attribute from the given target.
     *
     * @param target the object from which to extract the attribute
     * @return the extracted attribute
     * @throws Exception if any error occurs during the extraction process
     */
    R get(T target) throws Exception;
}
