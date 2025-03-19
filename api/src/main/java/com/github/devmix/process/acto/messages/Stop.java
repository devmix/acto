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

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a message to stop an Active Object or a chain of Active Objects.
 * <p>
 * This message can be used to signal that one or more Active Objects should terminate their processing. It
 * optionally includes a list of objects that have been visited in the process of stopping, and it can be marked as
 * 'force' which means the stop operation is mandatory and cannot be ignored by the Active Object.
 *
 * @author Sergey Grachev
 */
public final class Stop {

    /**
     * Indicates whether the stop operation should be forced (cannot be ignored).
     */
    @Getter
    private final boolean force;

    /**
     * A chain of Active Objects to which this stop message applies, if applicable. Can be null.
     */
    @Getter
    @Nullable
    private LinkedList<ActiveObject<?>> chain;

    /**
     * Set of object IDs that have been visited during the stopping process, if applicable. Can be null.
     */
    @Nullable
    private Set<Object> visited;

    /**
     * The source Active Object from which this stop message originates, if applicable. Can be null.
     */
    @Nullable
    @Getter
    private ActiveObject<?> source;

    /**
     * Constructs a new Stop message with the given force flag.
     *
     * @param force whether the stop operation should be forced (cannot be ignored)
     */
    private Stop(final boolean force) {
        this.force = force;
    }

    /**
     * Constructs a new Stop message with the given parameters.
     *
     * @param source  the Active Object from which this stop message originates
     * @param chain   a list of Active Objects to which this stop message applies
     * @param visited a set of object IDs that have been visited during the stopping process
     * @param force   whether the stop operation should be forced (cannot be ignored)
     */
    private Stop(final ActiveObject<?> source, final LinkedList<ActiveObject<?>> chain,
                 final Set<Object> visited, final boolean force) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.chain = Objects.requireNonNull(chain, "chain cannot be null");
        this.visited = new HashSet<>(Objects.requireNonNull(visited, "visited cannot be null"));
        this.force = force;
    }

    /**
     * Creates a new stop message that is an upstream continuation of the given stop message.
     * <p>
     * The new message will have the specified source Active Object, and it will inherit the chain,
     * visited set, and force flag from the original message.
     *
     * @param source  the new source Active Object for this stop message
     * @param message the original stop message to continue upstream from
     * @return a new Stop message that continues the stopping process upstream
     */
    public static Stop upstream(final ActiveObject<?> source, final Stop message) {
        return new Stop(source, message.chain, message.visited, message.force);
    }

    /**
     * Creates a new stop message with the force flag set to true.
     * <p>
     * The resulting stop operation will be mandatory and cannot be ignored by any Active Object.
     *
     * @return a new forced Stop message
     */
    public static Stop force() {
        return new Stop(true);
    }

    /**
     * Creates a new stop message with the force flag set to false.
     * <p>
     * The resulting stop operation can potentially be ignored by an Active Object, depending on its implementation.
     *
     * @return a new non-forced Stop message
     */
    public static Stop nonBlocked() {
        return new Stop(false);
    }

    /**
     * Checks whether the given object ID has been visited during the stopping process.
     *
     * @param objectId the object ID to check for in the visited set
     * @return true if the object ID has been visited, false otherwise
     */
    public boolean isVisited(final Object objectId) {
        return visited != null && visited.contains(objectId);
    }

    /**
     * Records an Active Object as part of the stopping process.
     * <p>
     * The specified object will be added to the chain, and its ID will be recorded in the visited set.
     *
     * @param object the Active Object to record as being stopped
     */
    public void stopping(final ActiveObject<?> object) {
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
