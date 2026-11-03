/*
 * devMix · Process · Active Objects [ActO]
 * Copyright (C) 2025, Sergey Grachev <sergey.grachev@yahoo.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.devmix.process.acto.core.registry;

import com.github.devmix.process.acto.ActiveObject;
import com.github.devmix.process.acto.ActiveObjectFactory;
import com.github.devmix.process.acto.ActiveObjectLifecycle;
import com.github.devmix.process.acto.ActiveObjectStatus;
import com.github.devmix.process.acto.ActiveObjectsDispatcher;
import com.github.devmix.process.acto.core.config.DispatcherConfig;
import com.github.devmix.process.acto.core.context.DefaultActiveObjectContext;
import com.github.devmix.process.acto.core.context.InternalDestroy;
import com.github.devmix.process.acto.core.context.StartGraph;
import com.github.devmix.process.acto.exceptions.RegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Sergey Grachev
 */
public class DefaultActiveObjectsDispatcher implements ActiveObjectsDispatcher {

    private final Logger log = LoggerFactory.getLogger(DefaultActiveObjectsDispatcher.class);

    private final Map<Class<?>, ActiveObjectFactory<?, ?, ?>> objectFactories = new ConcurrentHashMap<>();
    private final Map<Class<?>, ActiveObjectLifecycle<?>> objectLifecycles = new ConcurrentHashMap<>();
    private final Map<Object, DefaultActiveObjectContext<?>> contexts = new ConcurrentHashMap<>();
    private final Executor executor;
    private final StartGraph startGraph = new StartGraph();
    private final AtomicBoolean started = new AtomicBoolean();

    private volatile DispatcherConfig config;
    private volatile ScheduledExecutorService heartbeatExecutor;
    private volatile ScheduledFuture<?> heartbeatFuture;

    public DefaultActiveObjectsDispatcher() {
        this(DispatcherConfig.standard());
    }

    /**
     * @param heartbeatInterval heartbeat interval in milliseconds
     * @deprecated use {@link #DefaultActiveObjectsDispatcher(DispatcherConfig)}
     */
    @Deprecated
    public DefaultActiveObjectsDispatcher(final long heartbeatInterval) {
        this(DispatcherConfig.builder()
                .heartbeatInterval(java.time.Duration.ofMillis(heartbeatInterval))
                .build());
    }

    public DefaultActiveObjectsDispatcher(final DispatcherConfig config) {
        this.config = config;
        this.executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("acto-dispatch-", 0).factory());
        start();
    }

    /**
     * Returns the configuration of this dispatcher.
     *
     * @return the dispatcher configuration
     */
    public DispatcherConfig getConfig() {
        return config;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, I, O> ActiveObject<T> create(final Class<T> objectClass, final I id, final O options) {
        if (!started.get()) {
            throw new RegistryException("Registry has been stopped");
        }

        final var factory = (ActiveObjectFactory<T, I, O>) objectFactories.get(objectClass);
        final var lifecycle = (ActiveObjectLifecycle<T>) objectLifecycles.get(objectClass);

        if (factory == null && lifecycle == null) {
            throw new RegistryException("No factory or lifecycle for object with class: " + objectClass);
        }

        return (ActiveObject<T>) contexts.compute(id, (k, current) -> {
            checkExisting(k, current);

            log.debug("create new object: factory=[{}] lifecycle=[{}] id=[{}]", factory, lifecycle, k);

            final var instance = factory != null ? factory.create(id, options) : null;

            return new DefaultActiveObjectContext<T>(id, options, instance, lifecycle, this, executor, config, startGraph);
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, I, O> ActiveObject<T> create(final ActiveObjectFactory<T, I, O> factory, final I id, final O options) {
        if (!started.get()) {
            throw new RegistryException("Registry has been stopped");
        }

        return (ActiveObject<T>) contexts.compute(id, (k, current) -> {
            checkExisting(k, current);

            final var instance = factory.create(id, options);
            final var lifecycle = (ActiveObjectLifecycle<T>) objectLifecycles.get(instance.getClass());

            log.debug("create new object: factory=[{}] lifecycle=[{}] id=[{}]", factory, lifecycle, k);

            return new DefaultActiveObjectContext<T>(id, options, instance, lifecycle, this, executor, config, startGraph);
        });
    }

    @Nullable
    @Override
    public <T> ActiveObject<T> findById(final Object id) {
        //noinspection unchecked
        return (ActiveObject<T>) contexts.get(id);
    }

    @Override
    public <T> ActiveObject<T> getById(final Object id) {
        final var recipient = findById(id);
        if (recipient == null) {
            throw new RegistryException("Unknown active object: " + id);
        }
        //noinspection unchecked
        return (ActiveObject<T>) recipient;
    }

    /**
     * @return heartbeat interval in milliseconds
     */
    public long getHeartbeatInterval() {
        return config.getHeartbeatInterval().toMillis();
    }

    /**
     * @param heartbeatInterval heartbeat interval in milliseconds
     * @deprecated use {@link #DefaultActiveObjectsDispatcher(DispatcherConfig)}; changing the
     *         interval after start is applied to the running heartbeat but is not persisted
     *         to the configuration of already created contexts
     */
    @Deprecated
    public void setHeartbeatInterval(final long heartbeatInterval) {
        this.config = config.toBuilder()
                .heartbeatInterval(java.time.Duration.ofMillis(heartbeatInterval))
                .build();
        rescheduleHeartbeat();
    }

    @Override
    public long getIdleTimeout() {
        return config.getIdleTimeout().toNanos();
    }

    @Override
    public long getWaitMessagesTimeout() {
        return config.getWaitMessagesTimeout().toMillis();
    }

    @Override
    public <T, I, O> void registerFactory(final Class<T> objectClass, final ActiveObjectFactory<T, I, O> factory) {
        log.debug("register new object factory [{}:{}]", objectClass, factory);

        objectFactories.put(objectClass, factory);
    }

    @Override
    public <T, I, O> void registerLifecycle(final Class<T> objectClass, final ActiveObjectLifecycle<T> lifecycle) {
        log.debug("register new object lifecycle [{}:{}]", objectClass, lifecycle);

        objectLifecycles.put(objectClass, lifecycle);
    }

    @Override
    public void shutdown(final boolean force) {
        if (started.compareAndSet(true, false)) {
            try {
                for (final var context : new ArrayList<>(contexts.values())) {
                    if (!destroy(context.getId(), force) && !force) {
                        break;
                    }
                }
            } finally {
                stopHeartbeat();
            }
        }
    }

    @Override
    public void unregisterFactory(final Class<?> objectClass) {
        log.debug("unregister object factory [{}]", objectClass);

        objectFactories.remove(objectClass);
    }

    @Override
    public boolean destroy(final Object objectId, final boolean force) {
        final var context = contexts.get(objectId);
        if (context == null) {
            return false;
        }

        try {
            final var success = context.request(force ? InternalDestroy.force() : InternalDestroy.instance())
                    .get(getObjectStopTimeout(), TimeUnit.MILLISECONDS);

            if (Boolean.TRUE.equals(success)) {
                // remove only this context: a concurrent create() may have already replaced it
                contexts.remove(objectId, context);
                return true;
            }
            return false;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while destroying object: {}", objectId);
            return false;
        } catch (final TimeoutException | ExecutionException e) {
            log.error("Cannot destroy object: {}", objectId, e);
            return false;
        }
    }

    private long getObjectStopTimeout() {
        return config.getDestroyTimeout().toMillis();
    }

    private void checkExisting(final Object id, final DefaultActiveObjectContext<?> current) {
        if (current == null) {
            return;
        }

        final var status = current.getStatus();
        if (status == ActiveObjectStatus.DESTROYED) {
            log.debug("replace already destroyed object with ID=[{}]", id);
            return;
        }
        if (status == ActiveObjectStatus.DESTROYING) {
            throw new RegistryException("Object with ID=[" + id + "] is being destroyed");
        }
        throw new RegistryException("Object with ID=[" + id + "] already registered");
    }

    private void start() {
        if (started.compareAndSet(false, true)) {
            heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                final var thread = new Thread(r, "acto-heartbeat");
                thread.setDaemon(true);
                return thread;
            });
            rescheduleHeartbeat();
        }
    }

    private void rescheduleHeartbeat() {
        final var executor = heartbeatExecutor;
        if (executor == null || executor.isShutdown()) {
            return;
        }

        final var previous = heartbeatFuture;
        if (previous != null) {
            previous.cancel(false);
        }

        final var interval = Math.max(1L, config.getHeartbeatInterval().toMillis());
        heartbeatFuture = executor.scheduleWithFixedDelay(
                this::heartbeatOnce, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void heartbeatOnce() {
        if (!started.get()) {
            return;
        }

        for (final var context : contexts.values()) {
            if (!started.get()) {
                return;
            }
            try {
                context.heartbeat();
            } catch (final Exception e) {
                log.warn("heartbeat failed for [{}]", context.getId(), e);
            }
        }
    }

    private void stopHeartbeat() {
        final var executor = heartbeatExecutor;
        if (executor != null) {
            executor.shutdownNow();
            heartbeatExecutor = null;
            heartbeatFuture = null;
        }
    }
}
