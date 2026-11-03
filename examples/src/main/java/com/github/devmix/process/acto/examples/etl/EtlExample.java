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

package com.github.devmix.process.acto.examples.etl;

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Use case 2: an ETL pipeline (source -&gt; transformer -&gt; target) built from three
 * active objects wired with {@code DEPENDS_ON} dependencies.
 *
 * <p>Objects are created in an arbitrary order and nothing is started explicitly: a single
 * {@code load} message to the target starts the whole pipeline in dependency order and
 * moves the data end-to-end.</p>
 *
 * @author Sergey Grachev
 */
public final class EtlExample {

    public static final String ID_SOURCE = "etl:source";
    public static final String ID_TRANSFORMER = "etl:transformer";
    public static final String ID_TARGET = "etl:target";

    private EtlExample() {
    }

    public static void main(final String[] args) throws Exception {
        final var registry = new DefaultActiveObjectsDispatcher();
        final var startOrder = new CopyOnWriteArrayList<String>();

        // creation order is intentionally reversed: dependencies are resolved on demand
        final var target = registry.create(TargetEndpoint::new, ID_TARGET, startOrder);
        registry.create(TransformerEndpoint::new, ID_TRANSFORMER, startOrder);
        registry.create(SourceEndpoint::new, ID_SOURCE, startOrder);

        // one message to the target starts the whole pipeline and moves the data end-to-end
        final var loaded = target.request(TargetEndpoint.MSG_LOAD).get();
        System.out.println("start order: " + startOrder);
        System.out.println("loaded:      " + loaded);
        System.out.println("sink:        " + target.getInstance().sinkView());

        registry.shutdown(true);
    }
}
