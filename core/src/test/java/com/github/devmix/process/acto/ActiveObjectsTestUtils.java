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

package com.github.devmix.process.acto;

import lombok.experimental.UtilityClass;

/**
 * @author Sergey Grachev
 */
@UtilityClass
public final class ActiveObjectsTestUtils {

    public static boolean allWithStatus(final ActiveObjectStatus status, final ActiveObjectContext... contexts) {
        for (final var context : contexts) {
            if (context.getStatus() != status) {
                return false;
            }
        }
        return true;
    }
}
