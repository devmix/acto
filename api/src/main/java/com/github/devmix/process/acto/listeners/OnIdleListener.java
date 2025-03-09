package com.github.devmix.process.acto.listeners;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.messages.Idle;

/**
 * Represents a listener for idle events in an active object context.
 *
 * @param <T> the type of the object handled by this listener
 * @author Sergey Grachev
 */
public interface OnIdleListener<T> {

    /**
     * Called when an object is activated due to an idle event.
     *
     * @param message the activate message indicating that the object has been activated
     * @param context the active object context containing the object and related data
     * @return true if the listener handled the activation, false otherwise
     */
    boolean onObjectActivate(Idle.Activate message, ActiveObjectContext<T> context);

    /**
     * Called when an object is deactivated due to an idle event.
     *
     * @param message the deactivate message indicating that the object has been deactivated
     * @param context the active object context containing the object and related data
     * @return true if the listener handled the deactivation, false otherwise
     */
    boolean onObjectDeactivate(Idle.Deactivate message, ActiveObjectContext<T> context);
}
