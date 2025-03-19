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
import com.github.devmix.process.acto.messages.Stop;

/**
 * Interface for a listener that handles stop events in an active object context.
 *
 * @param <T> the type of the state object managed by the active object
 */
@FunctionalInterface
public interface OnStopListener<T> {

    /**
     * Called when the active object is about to be stopped.
     *
     * @param message the {@link Stop} message indicating the stop request
     * @param context the {@link ActiveObjectContext} containing the state of the active object
     */
    void onObjectStop(Stop message, ActiveObjectContext<T> context);
}
