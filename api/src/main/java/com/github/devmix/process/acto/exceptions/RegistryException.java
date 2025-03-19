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
 * Custom exception class representing a registry-related error in the Active Object pattern implementation.
 *
 * @author Sergey Grachev
 */
public class RegistryException extends ActiveObjectException {

    @Serial
    private static final long serialVersionUID = -8011900422294771563L;

    /**
     * Constructs a new {@code RegistryException} with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public RegistryException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code RegistryException} with the specified detail message and cause.
     * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated in
     * this exception's detail message.</p>
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *                A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public RegistryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
