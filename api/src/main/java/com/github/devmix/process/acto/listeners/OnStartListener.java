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
import com.github.devmix.process.acto.messages.Start;

/**
 * Represents a listener that is called when an object starts.
 *
 * <p>The {@link OnStartListener} interface is a functional interface, meaning it has
 * exactly one abstract method ({@code onObjectStart}). This allows instances of this
 * interface to be created with lambda expressions or method references.</p>
 *
 * @param <T> the type of the object being managed by the ActiveObjectContext
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface OnStartListener<T> {

    /**
     * Called when an object starts.
     *
     * <p>This method is invoked with a {@link Start} message and the associated
     * {@link ActiveObjectContext}. Implementations of this method should contain the logic to be
     * executed when the object starts.</p>
     *
     * @param message a {@link Start} message indicating that the object has started
     * @param context the {@link ActiveObjectContext} associated with the starting object
     */
    void onObjectStart(Start message, ActiveObjectContext<T> context);
}
