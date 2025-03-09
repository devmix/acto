package com.github.devmix.process.acto.core.registry;

import com.github.devmix.process.acto.ActiveObject;
import com.github.devmix.process.acto.ActiveObjectFactory;
import com.github.devmix.process.acto.ActiveObjectLifecycle;
import com.github.devmix.process.acto.ActiveObjectsDispatcher;
import com.github.devmix.process.acto.core.context.DefaultActiveObjectContext;
import com.github.devmix.process.acto.core.context.InternalDestroy;
import com.github.devmix.process.acto.exceptions.RegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
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
    private final ForkJoinPool executor;
    private final AtomicBoolean started = new AtomicBoolean();

    private HeartbeatTask heartbeatTask;
    private long heartbeatInterval;

    public DefaultActiveObjectsDispatcher() {
        this(TimeUnit.MINUTES.toMillis(5));
    }

    public DefaultActiveObjectsDispatcher(final long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
        this.executor = new ForkJoinPool();
        start();
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
            if (current != null) {
                throw new RegistryException("Object with ID=[" + k + "] already registered");
            }

            log.debug("create new object: factory=[{}] lifecycle=[{}] id=[{}]", factory, lifecycle, k);

            final var instance = factory != null ? factory.create(id, options) : null;

            return new DefaultActiveObjectContext<T>(id, options, instance, lifecycle, this, executor);
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, I, O> ActiveObject<T> create(final ActiveObjectFactory<T, I, O> factory, final I id, final O options) {
        if (!started.get()) {
            throw new RegistryException("Registry has been stopped");
        }

        return (ActiveObject<T>) contexts.compute(id, (k, current) -> {
            if (current != null) {
                throw new RegistryException("Object with ID=[" + k + "] already registered");
            }

            final var instance = factory.create(id, options);
            final var lifecycle = (ActiveObjectLifecycle<T>) objectLifecycles.get(instance.getClass());

            log.debug("create new object: factory=[{}] lifecycle=[{}] id=[{}]", factory, lifecycle, k);

            return new DefaultActiveObjectContext<T>(id, options, instance, lifecycle, this, executor);
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

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(final long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    @Override
    public long getIdleTimeout() {
        return TimeUnit.MINUTES.toNanos(5);
    }

    @Override
    public long getWaitMessagesTimeout() {
        return 500;
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
            boolean success = true;
            for (final var context : new ArrayList<>(contexts.values())) {
                if (!destroy(context.getId(), force) && !force) {
                    success = false;
                    break;
                }
            }

            if (success) {
                heartbeatTask.cancel(true);
                heartbeatTask = null;
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
        var context = contexts.remove(objectId);
        if (context != null) {
            try {
                final var success = context.request(InternalDestroy.instance())
                        .get(getObjectStopTimeout(), TimeUnit.MILLISECONDS);

                if (Boolean.TRUE.equals(success)) {
                    context = null;
                }
            } catch (final InterruptedException | TimeoutException | ExecutionException e) {
                log.error("Cannot destroy object", e);
                if (!force) {
                    throw new RegistryException(e.getMessage(), e);
                }
            } finally {
                if (context != null) {
                    contexts.put(context.getId(), context);
                }
            }
        }
        return context == null;
    }

    private long getObjectStopTimeout() {
        return TimeUnit.SECONDS.toMillis(15);
    }

    private void start() {
        if (started.compareAndSet(false, true)) {
            heartbeatTask = (HeartbeatTask) executor.submit(new HeartbeatTask());
        }
    }

    private class HeartbeatTask extends ForkJoinTask<Void> {

        @Serial
        private static final long serialVersionUID = 3127215789445995259L;

        @Override
        public Void getRawResult() {
            return null;
        }

        @Override
        protected void setRawResult(final Void value) {

        }

        @Override
        protected boolean exec() {
            while (started.get()) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(TimeUnit.MILLISECONDS.toMillis(heartbeatInterval));
                } catch (final InterruptedException e) {
                    break;
                }

                for (final var context : contexts.values()) {
                    if (started.get()) {
                        context.heartbeat();
                    }
                }
            }
            return true;
        }
    }
}
