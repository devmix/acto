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
 * Public API of the Active Objects pattern: dispatcher and object context contracts,
 * lifecycle and message types, exceptions and observability hooks.
 *
 * @author Sergey Grachev
 */
module devmix.process.acto.api {
    exports com.github.devmix.process.acto;
    exports com.github.devmix.process.acto.exceptions;
    exports com.github.devmix.process.acto.listeners;
    exports com.github.devmix.process.acto.messages;

    requires org.slf4j;
    requires static jsr305;
    requires static lombok;
}
