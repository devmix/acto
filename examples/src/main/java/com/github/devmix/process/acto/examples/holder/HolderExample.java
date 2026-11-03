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

package com.github.devmix.process.acto.examples.holder;

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Use case 3: a non-thread-safe file handle behind an active object. Eight threads append
 * lines concurrently; the writer itself is touched only by the object dispatch thread —
 * thread safety without a single {@code synchronized}.
 *
 * @author Sergey Grachev
 */
public final class HolderExample {

    private HolderExample() {
    }

    public static void main(final String[] args) throws Exception {
        final var file = Path.of(System.getProperty("java.io.tmpdir"), "acto-holder-example.log");
        Files.deleteIfExists(file);

        final var registry = new DefaultActiveObjectsDispatcher();
        final var holder = registry.create(FileWriterHolder::new, "writer", file);

        final var producers = new ArrayList<Thread>();
        for (var producer = 0; producer < 8; producer++) {
            final var id = producer;
            producers.add(Thread.ofVirtual().start(() -> {
                for (var i = 0; i < 50; i++) {
                    holder.requestAndForget(new FileWriterHolder.Append("producer-" + id + " line-" + i));
                }
            }));
        }
        for (final var producer : producers) {
            producer.join();
        }

        final var lines = holder.<java.util.List<String>>request(new FileWriterHolder.ReadAll()).get();
        System.out.println("lines written: " + lines.size());
        System.out.println("first: " + lines.get(0));

        registry.destroy("writer", false);
        registry.shutdown(true);
    }
}
