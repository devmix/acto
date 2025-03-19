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

package com.github.devmix.process.acto.messages;

import com.github.devmix.process.acto.ActiveObject;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a message to start processing with an Active Object.
 * This class holds information about the source of the message,
 * the chain of Active Objects involved in the process, and any visited objects.
 *
 * @author Sergey Grachev
 */
public final class Start {

    /**
     * A linked list representing the chain of active objects to be processed.
     * Nullable if no chain is provided.
     */
    @Getter
    @Nullable
    private LinkedList<ActiveObject<?>> chain;

    /**
     * A set of visited object IDs to prevent re-processing.
     * Nullable if no visits have been recorded yet.
     */
    @Nullable
    private Set<Object> visited;

    /**
     * The source active object from which the processing starts.
     * Nullable if not provided.
     */
    @Getter
    @Nullable
    private ActiveObject<?> source;

    /**
     * A boolean indicating whether the start is optional or mandatory.
     * This can be used to control certain aspects of message handling.
     *
     * @see #setOptional(boolean)
     */
    @Getter
    @Setter
    private boolean optional;

    private Start() {
    }

    /**
     * Constructor for creating a new Start instance with provided parameters.
     *
     * @param source   the source active object from which to start processing
     * @param chain    the linked list of active objects involved in the process
     * @param visited  the set of visited object IDs
     * @param optional whether the start is optional or mandatory
     */
    private Start(final ActiveObject<?> source, final LinkedList<ActiveObject<?>> chain,
                  final Set<Object> visited, final boolean optional) {
        this.source = Objects.requireNonNull(source, "Source cannot be null");
        this.chain = Objects.requireNonNull(chain, "Chain cannot be null");
        this.visited = Objects.requireNonNull(visited, "Visited set cannot be null");
        this.optional = optional;
    }

    /**
     * Creates a standard Start instance without any parameters.
     *
     * @return a new Start instance
     */
    public static Start standard() {
        return new Start();
    }

    /**
     * Creates a new Start instance based on an existing one, with the provided source and optionality flag.
     *
     * @param source   the source active object from which to start processing
     * @param origin   the original Start instance whose chain and visited objects will be copied
     * @param optional whether the start is optional or mandatory
     * @return a new Start instance based on the provided parameters
     */
    public static Start downstream(final ActiveObject<?> source, final Start origin, final boolean optional) {
        return new Start(source, origin.chain, origin.visited, optional);
    }

    /**
     * Checks if an object ID has already been visited during processing.
     *
     * @param objectId the object ID to check
     * @return true if the object ID is in the visited set; false otherwise
     */
    public boolean isVisited(final Object objectId) {
        return visited != null && visited.contains(objectId);
    }

    /**
     * Adds an active object to the chain and records its ID as visited.
     *
     * @param object the active object to add to the processing chain
     */
    public void starting(final ActiveObject<?> object) {
        if (chain == null) {
            chain = new LinkedList<>();
        }

        chain.add(object);

        if (visited == null) {
            visited = new HashSet<>(2);
        }

        visited.add(object.getId());
    }
}
