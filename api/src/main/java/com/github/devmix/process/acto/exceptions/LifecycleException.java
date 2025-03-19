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
 * Abstract exception class for lifecycle-related exceptions in an active object.
 *
 * @author Sergey Grachev
 */
abstract class LifecycleException extends ActiveObjectException {

    @Serial
    private static final long serialVersionUID = -2299680028039001891L;

    /**
     * Constructs a new lifecycle exception with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the getMessage() method).
     */
    LifecycleException(final String message) {
        super(message);
    }

    /**
     * Constructs a new lifecycle exception with the specified detail message and cause.
     *
     * <p>Note that the detail message associated with cause is not automatically incorporated in this
     * exception's detail message.</p>
     *
     * @param message The detail message (which is saved for later retrieval by the getMessage() method).
     * @param cause   The cause (which is saved for later retrieval by the getCause() method). A null value is
     *                permitted, and indicates that the cause is nonexistent or unknown.
     */
    LifecycleException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
