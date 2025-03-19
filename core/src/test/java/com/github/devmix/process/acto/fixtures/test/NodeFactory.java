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

package com.github.devmix.process.acto.fixtures.test;

import com.github.devmix.process.acto.ActiveObjectFactory;

import javax.annotation.Nullable;

/**
 * @author Sergey Grachev
 */
public final class NodeFactory implements ActiveObjectFactory<NodeObject, String, NodeOptions> {

    @Override
    public NodeObject create(final String id, @Nullable final NodeOptions options) {
        return new NodeObject(id, options);
    }
}
