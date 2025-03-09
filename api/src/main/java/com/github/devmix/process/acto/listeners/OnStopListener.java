package com.github.devmix.process.acto.listeners;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.messages.Stop;

/**
 * Interface for a listener that handles stop events in an active object context.
 *
 * @param <T> the type of the state object managed by the active object
 */
@FunctionalInterface
public interface OnStopListener<T> {

    /**
     * Called when the active object is about to be stopped.
     *
     * @param message the {@link Stop} message indicating the stop request
     * @param context the {@link ActiveObjectContext} containing the state of the active object
     */
    void onObjectStop(Stop message, ActiveObjectContext<T> context);
}
