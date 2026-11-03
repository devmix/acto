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

package com.github.devmix.process.acto.core.context;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks «object A is waiting for object B to start» edges while dependency starts are in flight.
 *
 * <p>The per-message {@code visited} set of the {@code Start} message detects cycles within a
 * single start cascade only. When two cascades are started independently (A waits for B while B
 * waits for A), neither cascade sees the other; the start graph makes such cross-start cycles
 * visible and lets them be rejected immediately instead of waiting for the dependency timeout.</p>
 *
 * @author Sergey Grachev
 */
public final class StartGraph {

    private final Map<Object, Set<Object>> waitingOn = new ConcurrentHashMap<>();

    /**
     * Registers that {@code from} is waiting for {@code to} to start.
     *
     * @param from the waiting object id
     * @param to   the object id being awaited
     */
    public void add(final Object from, final Object to) {
        waitingOn.computeIfAbsent(from, k -> ConcurrentHashMap.newKeySet()).add(to);
    }

    /**
     * Removes the «from waits for to» edge.
     *
     * @param from the waiting object id
     * @param to   the object id that is no longer awaited
     */
    public void remove(final Object from, final Object to) {
        final var edges = waitingOn.get(from);
        if (edges != null) {
            edges.remove(to);
            if (edges.isEmpty()) {
                waitingOn.remove(from, edges);
            }
        }
    }

    /**
     * Checks whether {@code from} transitively waits for {@code target}
     * (i.e. there is a path of waiting edges from {@code from} to {@code target}).
     *
     * @param from   the node to start the search from
     * @param target the node to look for
     * @return true if sending a start request from {@code target} to {@code from}
     *         would close a cycle
     */
    public boolean reaches(final Object from, final Object target) {
        if (from.equals(target)) {
            return true;
        }

        final var queue = new ArrayDeque<Object>();
        final var seen = new HashSet<>();
        queue.add(from);
        seen.add(from);

        while (!queue.isEmpty()) {
            final var current = queue.poll();
            final var edges = waitingOn.get(current);
            if (edges == null) {
                continue;
            }
            for (final var next : edges) {
                if (next.equals(target)) {
                    return true;
                }
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return false;
    }
}
