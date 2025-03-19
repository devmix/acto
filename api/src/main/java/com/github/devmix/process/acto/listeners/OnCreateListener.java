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

package com.github.devmix.process.acto.listeners;

import com.github.devmix.process.acto.ActiveObjectContext;

/**
 * Interface for listeners that are interested in object creation events within an Active Object context.
 *
 * @param <T> The type of the object managed by the Active Object context.
 */
@FunctionalInterface
public interface OnCreateListener<T> {

    /**
     * Method to be called when a new object is created within an Active Object context.
     *
     * @param context The ActiveObjectContext associated with the newly created object.
     */
    void onObjectCreate(ActiveObjectContext<T> context);
}
