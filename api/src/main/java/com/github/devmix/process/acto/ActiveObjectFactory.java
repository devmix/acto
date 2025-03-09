package com.github.devmix.process.acto;

import javax.annotation.Nullable;

/**
 * Factory interface for creating ActiveObject instances.
 *
 * <p>This interface defines a method to create an instance of type {@code T}
 * using an identifier of type {@code I} and optional options of type {@code O}.
 *
 * @param <T> the type of ActiveObject to be created
 * @param <I> the type of the identifier used for creating the ActiveObject
 * @param <O> the type of the optional options used during creation, can be null
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface ActiveObjectFactory<T, I, O> {

    /**
     * Creates an instance of an ActiveObject.
     *
     * @param id      the identifier for the ActiveObject to be created
     * @param options optional configuration options; may be null if not required
     * @return a new instance of the ActiveObject type {@code T}
     */
    T create(I id, @Nullable O options);
}
