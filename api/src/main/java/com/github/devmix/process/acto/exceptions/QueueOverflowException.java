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
 * Thrown when a new user message cannot be accepted because the object queue is full and
 * the configured overflow policy rejects it. For {@code request()} the future completes
 * with this exception; for {@code requestAndForget()} it is thrown to the caller.
 *
 * @author Sergey Grachev
 */
public final class QueueOverflowException extends ActiveObjectException {

    @Serial
    private static final long serialVersionUID = 6153298712447903518L;

    /**
     * Constructs a new {@code QueueOverflowException} with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the
     *                {@link #getMessage()} method).
     */
    public QueueOverflowException(final String message) {
        super(message);
    }
}
