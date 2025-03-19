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

package com.github.devmix.process.acto.core.utils;

import com.github.devmix.process.acto.ActiveObject;
import com.github.devmix.process.acto.ActiveObjectDependency;
import com.github.devmix.process.acto.ActiveObjectStatus;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.MapUtils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * Utility class for formatting information about ActiveObjects.
 *
 * @author Sergey Grachev
 */
@UtilityClass
public final class ObjectInfoUtils {

    /**
     * Generates a string representation of a chain of ActiveObject instances.
     * Each object's ID is included in the output, separated by " > ".
     *
     * @param chain The collection of ActiveObjects to unroll into a chain.
     *              If null, returns an empty bracketed string "[]".
     * @return A string representation of the chain of ActiveObject IDs.
     */
    public static String unrollChain(@Nullable final Collection<ActiveObject<?>> chain) {
        if (chain == null) {
            return "[]";
        }

        final var result = new StringBuilder("[");
        for (final var object : chain) {
            if (result.length() > 1) {
                result.append(" > ");
            }
            result.append(object.getId());
        }

        return result.append(']').toString();
    }

    /**
     * Generates a detailed string representation of an ActiveObject's state,
     * including its ID, status, and dependencies.
     *
     * @param id           The unique identifier of the ActiveObject.
     * @param dependencies A map of dependencies for this ActiveObject, where keys are
     *                     dependency identifiers and values are instances of ActiveObjectDependency.
     * @param status       The current status of the ActiveObject.
     * @return A detailed string representation of the ActiveObject's state.
     */
    public static String printObjectState(
            final Object id, final Map<Object, ? extends ActiveObjectDependency> dependencies, final ActiveObjectStatus status) {
        final var result = new StringBuilder("\n\tid:").append(id).append(", status:").append(status);

        if (MapUtils.isNotEmpty(dependencies)) {
            result.append(", dependencies:\n");
            for (final var entry : dependencies.entrySet()) {
                final var dependency = entry.getValue();
                result.append("\t\t").append(entry.getKey()).append(" - ")
                        .append("type:").append(dependency.getType())
                        .append(", resolved:").append(dependency.isResolved())
                        .append(", optional:").append(dependency.isOptional());

                final var lastError = dependency.getLastError();
                if (lastError != null) {
                    result.append(", error:").append(lastError);
                }

                result.append("\n");
            }
        }
        return result.toString();
    }
}
