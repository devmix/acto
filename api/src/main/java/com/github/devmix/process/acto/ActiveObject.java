package com.github.devmix.process.acto;

import com.github.devmix.process.acto.messages.AttributeGetter;
import com.github.devmix.process.acto.messages.AttributeSetter;
import com.github.devmix.process.acto.messages.Invoke;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an active object that encapsulates its state and processes requests asynchronously.
 *
 * @param <T> the type of the encapsulated instance
 * @author Sergey Grachev
 */
public interface ActiveObject<T> {

    /**
     * Returns a unique identifier for this active object.
     *
     * @return the ID of this active object
     */
    Object getId();

    /**
     * Returns the encapsulated instance managed by this active object.
     *
     * @return the encapsulated instance
     */
    T getInstance();

    /**
     * Asynchronously retrieves an attribute from the encapsulated instance using the provided {@link AttributeGetter}.
     *
     * @param <R>    the type of the attribute to get
     * @param action the action that retrieves the attribute
     * @return a CompletableFuture containing the retrieved attribute value
     */
    <R> CompletableFuture<R> get(AttributeGetter<T, R> action);

    /**
     * Asynchronously sets an attribute in the encapsulated instance using the provided {@link AttributeSetter}.
     *
     * @param <V>    the type of the attribute to set
     * @param action the action that sets the attribute
     * @param value  the new value for the attribute, or null if applicable
     * @return a CompletableFuture indicating completion of the operation
     */
    <V> CompletableFuture<Void> set(AttributeSetter<T, V> action, @Nullable V value);

    /**
     * Asynchronously sets an attribute in the encapsulated instance using the provided {@link AttributeSetter},
     * without waiting for the operation to complete.
     *
     * @param <V>    the type of the attribute to set
     * @param action the action that sets the attribute
     * @param value  the new value for the attribute, or null if applicable
     */
    <V> void setAndForget(AttributeSetter<T, V> action, @Nullable V value);

    /**
     * Asynchronously invokes a method on the encapsulated instance using the provided {@link Invoke}.
     *
     * @param invoke the invocation action to perform
     * @return a CompletableFuture indicating completion of the operation
     */
    CompletableFuture<Void> invoke(Invoke<T> invoke);

    /**
     * Asynchronously invokes a method on the encapsulated instance using the provided {@link Invoke},
     * without waiting for the operation to complete.
     *
     * @param invoke the invocation action to perform
     */
    void invokeAnForget(Invoke<T> invoke);

    /**
     * Asynchronously processes a request message, returning the result.
     *
     * @param <R>     the type of the result
     * @param message the request message to process
     * @return a CompletableFuture containing the result of processing the request
     */
    <R> CompletableFuture<R> request(Object message);

    /**
     * Asynchronously processes a request message without waiting for the result.
     *
     * @param message the request message to process
     */
    void requestAndForget(Object message);
}
