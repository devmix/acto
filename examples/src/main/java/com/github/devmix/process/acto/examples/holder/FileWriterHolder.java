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

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnDestroyListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;
import com.github.devmix.process.acto.listeners.OnStartListener;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * A non-thread-safe resource (a {@link BufferedWriter} on a file) hidden behind an active
 * object. The writer is opened on start, written on every append and closed on destroy —
 * all exclusively from the object dispatch thread, so the example contains no locks at all.
 *
 * @author Sergey Grachev
 */
public class FileWriterHolder implements OnStartListener<FileWriterHolder>,
        OnMessageListener<FileWriterHolder>, OnDestroyListener<FileWriterHolder> {

    public record Append(String line) {
    }

    public record ReadAll() {
    }

    private final Path path;
    private BufferedWriter writer;

    public FileWriterHolder(final Object id, final Path path) {
        this.path = path;
    }

    @Override
    public void onObjectStart(final com.github.devmix.process.acto.messages.Start message,
                              final ActiveObjectContext<FileWriterHolder> context) {
        try {
            writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<FileWriterHolder> context) {
        try {
            if (message instanceof Append append) {
                writer.write(append.line());
                writer.newLine();
                return null;
            }
            if (message instanceof ReadAll) {
                writer.flush();
                return Files.readAllLines(path);
            }
            return null;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void onObjectDestroy(final ActiveObjectContext<FileWriterHolder> context) {
        if (writer != null) {
            try {
                writer.close();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
