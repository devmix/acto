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

import javax.annotation.Nullable;

/**
 * Functional interface that represents a listener for messages.
 * <p>
 * The implementing class should define the behavior of how to handle a message within the context.
 *
 * @param <T> the type of object stored in the ActiveObjectContext
 */
@FunctionalInterface
public interface OnMessageListener<T> {

    /**
     * This method is called when an object message is received.
     * <p>
     * The implementing class should define how to handle the message based on the provided context.
     *
     * @param message the message that was received, can be any type of Object
     * @param context the ActiveObjectContext in which the message was received
     * @return an object representing the result or response to the message,
     * or null if there is no specific response required
     */
    @Nullable
    Object onObjectMessage(Object message, ActiveObjectContext<T> context);
}
