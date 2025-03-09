package com.github.devmix.process.acto.listeners;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.messages.Start;

/**
 * Represents a listener that is called when an object starts.
 *
 * <p>The {@link OnStartListener} interface is a functional interface, meaning it has
 * exactly one abstract method ({@code onObjectStart}). This allows instances of this
 * interface to be created with lambda expressions or method references.</p>
 *
 * @param <T> the type of the object being managed by the ActiveObjectContext
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface OnStartListener<T> {

    /**
     * Called when an object starts.
     *
     * <p>This method is invoked with a {@link Start} message and the associated
     * {@link ActiveObjectContext}. Implementations of this method should contain the logic to be
     * executed when the object starts.</p>
     *
     * @param message a {@link Start} message indicating that the object has started
     * @param context the {@link ActiveObjectContext} associated with the starting object
     */
    void onObjectStart(Start message, ActiveObjectContext<T> context);
}
