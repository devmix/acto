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

/**
 * Default implementation of the Active Objects dispatcher. The context and utility packages
 * are implementation details and are not exported.
 *
 * @author Sergey Grachev
 */
module devmix.process.acto.core {
    exports com.github.devmix.process.acto.core.config;
    exports com.github.devmix.process.acto.core.metrics;
    exports com.github.devmix.process.acto.core.registry;

    requires transitive devmix.process.acto.api;
    requires org.slf4j;
    requires org.apache.commons.collections4;
    // optional at runtime: only MicrometerDispatcherMetrics needs it
    requires static micrometer.core;
    requires static jsr305;
    requires static lombok;
}
