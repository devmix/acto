package com.github.devmix.process.acto.listeners;

import com.github.devmix.process.acto.ActiveObjectContext;

import javax.annotation.Nullable;

/**
 * Functional interface that represents a listener for messages.
 * <p>
 * The implementing class should define the behavior of how to handle a message within the context.
 *
 * @param <T> the type of object stored in the ActiveObjectContext
 */
@FunctionalInterface
public interface OnMessageListener<T> {

    /**
     * This method is called when an object message is received.
     * <p>
     * The implementing class should define how to handle the message based on the provided context.
     *
     * @param message the message that was received, can be any type of Object
     * @param context the ActiveObjectContext in which the message was received
     * @return an object representing the result or response to the message,
     * or null if there is no specific response required
     */
    @Nullable
    Object onObjectMessage(Object message, ActiveObjectContext<T> context);
}
