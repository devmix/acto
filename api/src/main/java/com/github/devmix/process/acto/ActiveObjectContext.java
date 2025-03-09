package com.github.devmix.process.acto;

import com.github.devmix.process.acto.exceptions.RegistryException;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Represents the context of an active object, providing methods to manage dependencies,
 * lifecycle, and message handling.
 *
 * @param <T> The type of instance held by the active object
 * @author Sergey Grachev
 */
public interface ActiveObjectContext<T> {

    /**
     * Returns a unique identifier for this active object.
     *
     * @return The identifier of the active object
     */
    Object getId();

    /**
     * Adds a dependency on another active object with specified optional flag.
     *
     * @param objectId The identifier of the active object to depend on
     * @param optional True if the dependency is optional, false otherwise
     * @return An {@link ActiveObjectDependency} representing this dependency
     */
    ActiveObjectDependency addDependsOn(Object objectId, boolean optional);

    /**
     * Waits until the message queue of this active object becomes empty.
     *
     * @param timeout The maximum time to wait
     * @param unit    The time unit for the specified timeout
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws ExecutionException   if an exception occurs during the execution
     * @throws TimeoutException     if the specified waiting time elapses before completion
     */
    void awaitEmptyQueue(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Retrieves a dependency on another active object by its identifier.
     *
     * @param objectId The identifier of the dependent active object
     * @return An {@link ActiveObjectDependency} representing this dependency,
     * or null if no such dependency exists
     */
    ActiveObjectDependency getDependsOn(Object objectId);

    /**
     * Finds a dependency on another active object by its identifier.
     *
     * @param objectId The identifier of the dependent active object
     * @return An {@link ActiveObjectDependency} representing this dependency,
     * or null if no such dependency exists
     */
    @Nullable
    ActiveObjectDependency findDependsOn(Object objectId);

    /**
     * Finds a required dependency for another active object by its identifier.
     *
     * @param objectId The identifier of the dependent active object
     * @return An {@link ActiveObjectDependency} representing this required dependency,
     * or null if no such dependency exists
     */
    @Nullable
    ActiveObjectDependency findRequiredFor(Object objectId);

    /**
     * Applies a function to each dependency, optionally filtering by type.
     *
     * @param fn           The consumer function to apply to each dependency
     * @param filterByType The type of dependencies to include,
     *                     or null if no filtering should be applied
     */
    void forEachDependency(Consumer<ActiveObjectDependency> fn, @Nullable ActiveObjectDependency.Type filterByType);

    /**
     * Returns the current idle timeout for this active object.
     *
     * @return The idle timeout in milliseconds
     */
    long getIdleTimeout();

    /**
     * Sets a new instance for this active object.
     *
     * @param instance The new instance, or null to clear the instance
     */
    void setInstance(@Nullable T instance);

    /**
     * Returns the current instance held by this active object.
     *
     * @return The instance, or null if no instance is currently set
     */
    T getInstance();

    /**
     * Returns the options associated with this active object context.
     *
     * @param <O> The type of options
     * @return The options
     */
    <O> O getOptions();

    /**
     * Sets a new idle timeout for this active object.
     *
     * @param idleTimeout The new idle timeout in milliseconds
     */
    void setIdleTimeout(long idleTimeout);

    /**
     * Returns the current status of this active object.
     *
     * @return The status of the active object
     */
    ActiveObjectStatus getStatus();

    /**
     * Returns the current wait messages timeout for this active object.
     *
     * @return The wait messages timeout in milliseconds
     */
    long getWaitMessagesTimeout();

    /**
     * Sets a new wait messages timeout for this active object.
     *
     * @param waitMessagesTimeout The new wait messages timeout in milliseconds
     */
    void setWaitMessagesTimeout(long waitMessagesTimeout);

    /**
     * Redirects a request to another active object and forgets it (does not expect response).
     *
     * @param objectId The identifier of the target active object
     * @param message  The message to send
     * @throws RegistryException if an error occurs during redirection
     */
    void redirectRequestAndForget(Object objectId, Object message) throws RegistryException;

    /**
     * Redirects a request to another active object and returns a future for its response.
     *
     * @param <R>      The type of the response
     * @param objectId The identifier of the target active object
     * @param message  The message to send
     * @return A completable future representing the eventual response
     * @throws RegistryException if an error occurs during redirection
     */
    <R> CompletableFuture<R> redirectRequest(Object objectId, Object message) throws RegistryException;
}
