package com.github.devmix.process.acto.listeners;

import com.github.devmix.process.acto.ActiveObjectContext;

/**
 * This functional interface defines a contract for listeners that need to be notified when an object is destroyed.
 *
 * @param <T> The type of the object being managed by the ActiveObjectContext.
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface OnDestroyListener<T> {

    /**
     * This method is called when an object is destroyed.
     *
     * @param context An instance of ActiveObjectContext that contains information about the destroyed object.
     */
    void onObjectDestroy(ActiveObjectContext<T> context);
}
