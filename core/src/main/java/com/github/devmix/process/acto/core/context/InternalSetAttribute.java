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

import com.github.devmix.process.acto.messages.AttributeSetter;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * @author Sergey Grachev
 */
@Getter
class InternalSetAttribute {

    private final AttributeSetter<Object, Object> action;
    private final @Nullable Object value;

    private InternalSetAttribute(final AttributeSetter<Object, Object> action, @Nullable final Object value) {
        this.action = action;
        this.value = value;
    }

    public static Object create(final AttributeSetter<?, ?> action, @Nullable final Object value) {
        //noinspection unchecked
        return new InternalSetAttribute((AttributeSetter<Object, Object>) action, value);
    }
}
