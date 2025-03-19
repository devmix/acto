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
 * Functional interface representing an operation that accepts a single input argument and performs some action.
 *
 * @param <T> the type of the input to the {@code run} method
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface Invoke<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param object the input argument
     * @throws Exception if an error occurs while performing the operation
     */
    void run(T object) throws Exception;
}
