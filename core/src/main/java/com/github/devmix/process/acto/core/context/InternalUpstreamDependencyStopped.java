package com.github.devmix.process.acto.core.context;

import com.github.devmix.process.acto.ActiveObject;
import lombok.Getter;

/**
 * @author Sergey Grachev
 */
final class InternalUpstreamDependencyStopped {

    @Getter
    private final ActiveObject<?> source;

    private InternalUpstreamDependencyStopped(final ActiveObject<?> source) {
        this.source = source;
    }

    public static InternalUpstreamDependencyStopped source(final ActiveObject<?> object) {
        return new InternalUpstreamDependencyStopped(object);
    }
}
