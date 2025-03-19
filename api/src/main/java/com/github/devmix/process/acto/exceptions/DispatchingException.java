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
 * Represents an exception that occurs during the dispatching process of an active object.
 *
 * @author Sergey Grachev
 */
public final class DispatchingException extends ActiveObjectException {

    @Serial
    private static final long serialVersionUID = -496097685663449702L;

    /**
     * Constructs a new {@code DispatchingException} with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public DispatchingException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code DispatchingException} with the specified detail message and cause.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause()} method). A null value
     *                is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public DispatchingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
