package com.github.devmix.process.acto.listeners;

import com.github.devmix.process.acto.ActiveObjectContext;

/**
 * Interface for listeners that are interested in object creation events within an Active Object context.
 *
 * @param <T> The type of the object managed by the Active Object context.
 */
@FunctionalInterface
public interface OnCreateListener<T> {

    /**
     * Method to be called when a new object is created within an Active Object context.
     *
     * @param context The ActiveObjectContext associated with the newly created object.
     */
    void onObjectCreate(ActiveObjectContext<T> context);
}
