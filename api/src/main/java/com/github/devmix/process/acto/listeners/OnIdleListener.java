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
import com.github.devmix.process.acto.messages.Idle;

/**
 * Represents a listener for idle events in an active object context.
 *
 * @param <T> the type of the object handled by this listener
 * @author Sergey Grachev
 */
public interface OnIdleListener<T> {

    /**
     * Called when an object is activated due to an idle event.
     *
     * @param message the activate message indicating that the object has been activated
     * @param context the active object context containing the object and related data
     * @return true if the listener handled the activation, false otherwise
     */
    boolean onObjectActivate(Idle.Activate message, ActiveObjectContext<T> context);

    /**
     * Called when an object is deactivated due to an idle event.
     *
     * @param message the deactivate message indicating that the object has been deactivated
     * @param context the active object context containing the object and related data
     * @return true if the listener handled the deactivation, false otherwise
     */
    boolean onObjectDeactivate(Idle.Deactivate message, ActiveObjectContext<T> context);
}
