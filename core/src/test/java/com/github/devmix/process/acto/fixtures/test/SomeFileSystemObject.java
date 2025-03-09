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
