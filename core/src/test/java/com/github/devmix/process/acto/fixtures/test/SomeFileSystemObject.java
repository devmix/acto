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

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Sergey Grachev
 */
public class SomeFileSystemObject implements OnMessageListener {

    private static final AtomicLong COUNTER = new AtomicLong(0);

    private final Path path;

    public SomeFileSystemObject(final String id, final String path) {
        this.path = Paths.get(requireNonNull(path));
    }

    public long getCounter() {
        return COUNTER.get();
    }

    public String getFilesList() throws IOException {
        try (final var list = Files.list(path)) {
            return list.map(next -> next.getFileName().toString()).collect(Collectors.joining("\n"));
        }
    }

    @Nullable
    @Override
    public Object onObjectMessage(final Object message, ActiveObjectContext context) {
        return "reflection object: " + COUNTER.getAndIncrement();
    }
}
