package com.github.devmix.process.acto;

import javax.annotation.Nullable;

/**
 * The ActiveObjectsDispatcher interface defines methods for managing active objects and their lifecycle in a system.
 *
 * @author Sergey Grachev
 */
public interface ActiveObjectsDispatcher {

    /**
     * Creates an instance of an active object using the provided factory, id, and options.
     *
     * @param <T>     The type of the active object to be created.
     * @param <I>     The type of the identifier used for the active object.
     * @param <O>     The type of the options used for the active object.
     * @param factory An implementation of ActiveObjectFactory which will create the instance of the active object.
     * @param id      The unique identifier for the active object.
     * @param options Options that can be used by the factory to configure the active object.
     * @return A new instance of the active object.
     */
    <T, I, O> ActiveObject<T> create(ActiveObjectFactory<T, I, O> factory, I id, O options);

    /**
     * Creates an instance of an active object using the provided class type, id, and options.
     *
     * @param <T>         The type of the active object to be created.
     * @param <I>         The type of the identifier used for the active object.
     * @param <O>         The type of the options used for the active object.
     * @param objectClass The class type of the active object to create.
     * @param id          The unique identifier for the active object.
     * @param options     Options that can be used by the factory to configure the active object.
     * @return A new instance of the active object.
     */
    <T, I, O> ActiveObject<T> create(Class<T> objectClass, I id, O options);

    /**
     * Finds an active object by its ID. If no active object is found with that ID, returns null.
     *
     * @param id The unique identifier of the active object to find.
     * @return An instance of ActiveObject if found; otherwise, null.
     */
    @Nullable
    <T> ActiveObject<T> findById(Object id);

    /**
     * Finds an active object by its ID. If no active object is found with that ID,
     * this method throws an exception.
     *
     * @param id The unique identifier of the active object to find.
     * @return An instance of ActiveObject if found.
     * @throws IllegalArgumentException If no active object is found with the specified ID.
     */
    <T> ActiveObject<T> getById(Object id);

    /**
     * Returns the idle timeout for an active object in nanoseconds. The maximum value
     * that can be returned by this method is 106751 days.
     *
     * @return Idle timeout in nanoseconds.
     */
    long getIdleTimeout();

    /**
     * Returns the wait messages timeout for an active object in nanoseconds.
     * This is the time the system waits for messages to be processed by an active object.
     *
     * @return Wait messages timeout in nanoseconds.
     */
    long getWaitMessagesTimeout();

    /**
     * Registers a factory with this dispatcher, which will be used to create instances of
     * the specified class type.
     *
     * @param <T>         The type of active object for which the factory is being registered.
     * @param <I>         The type of identifier used by this factory.
     * @param <O>         The type of options used by this factory.
     * @param objectClass The class type of the active objects created by the specified factory.
     * @param factory     The ActiveObjectFactory instance which will be registered with this dispatcher.
     */
    <T, I, O> void registerFactory(Class<T> objectClass, ActiveObjectFactory<T, I, O> factory);

    /**
     * Registers a lifecycle callback for the specified class type. This callback will be invoked
     * when active objects of that type are created or destroyed.
     *
     * @param <T>         The type of active object for which the lifecycle is being registered.
     * @param objectClass The class type of the active objects for which this lifecycle will be used.
     * @param lifecycle   An instance of ActiveObjectLifecycle to register.
     */
    <T, I, O> void registerLifecycle(Class<T> objectClass, ActiveObjectLifecycle<T> lifecycle);

    /**
     * Shuts down all active objects managed by this dispatcher. If force is true, it will forcibly
     * terminate all active objects; otherwise, it will attempt to shut them down gracefully.
     *
     * @param force Indicates whether to forcefully shutdown the active objects (true) or not (false).
     */
    void shutdown(boolean force);

    /**
     * Unregisters a previously registered factory for the specified class type. This means that this dispatcher
     * will no longer use this factory to create instances of that class.
     *
     * @param objectClass The class type for which the factory is being unregistered.
     */
    void unregisterFactory(Class<?> objectClass);

    /**
     * Destroys an active object with the specified ID. If force is true, it will forcibly terminate
     * the active object; otherwise, it will attempt to destroy it gracefully.
     *
     * @param objectId The unique identifier of the active object to be destroyed.
     * @param force    Indicates whether to forcefully destroy the active object (true) or not (false).
     * @return True if the active object was successfully destroyed; otherwise, false.
     */
    boolean destroy(Object objectId, boolean force);
}
