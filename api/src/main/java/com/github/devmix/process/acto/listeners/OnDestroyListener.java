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
 * This functional interface defines a contract for listeners that need to be notified when an object is destroyed.
 *
 * @param <T> The type of the object being managed by the ActiveObjectContext.
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface OnDestroyListener<T> {

    /**
     * This method is called when an object is destroyed.
     *
     * @param context An instance of ActiveObjectContext that contains information about the destroyed object.
     */
    void onObjectDestroy(ActiveObjectContext<T> context);
}
