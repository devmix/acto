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
 * Functional interface representing an operation that takes a single argument and returns a result,
 * allowing for checked exceptions to be thrown during its execution.
 *
 * @param <T> the type of the input to the run method
 * @param <R> the type of the result of the run method
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface InvokeAndGet<T, R> {

    /**
     * Executes the operation using the specified argument.
     *
     * @param object the input argument to the operation
     * @return the result of the operation
     * @throws Exception if an error occurs during the execution of the operation
     */
    R run(T object) throws Exception;
}
