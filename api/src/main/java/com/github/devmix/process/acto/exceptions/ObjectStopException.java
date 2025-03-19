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

package com.github.devmix.process.acto.exceptions;

import java.io.Serial;

/**
 * An exception indicating that an object failed to stop due to a blocking condition or external factor.
 *
 * @author Sergey Grachev
 */
public class ObjectStopException extends LifecycleException {

    @Serial
    private static final long serialVersionUID = -2995189569422394680L;

    /**
     * Constructs an instance of <code>ObjectStopException</code> with the specified detail message and the ID of the object that blocked the stop.
     *
     * @param message     The detail message.
     * @param blockedById The ID of the object that blocked the stop.
     */
    public ObjectStopException(final String message, final Object blockedById) {
        super(message);
    }
}
