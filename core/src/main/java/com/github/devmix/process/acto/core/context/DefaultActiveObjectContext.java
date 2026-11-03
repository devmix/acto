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

package com.github.devmix.process.acto.core.context;

import com.github.devmix.process.acto.ActiveObject;
import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.ActiveObjectDependency;
import com.github.devmix.process.acto.ActiveObjectLifecycle;
import com.github.devmix.process.acto.ActiveObjectStatus;
import com.github.devmix.process.acto.ActiveObjectsDispatcher;
import com.github.devmix.process.acto.core.config.DispatcherConfig;
import com.github.devmix.process.acto.core.config.QueueOverflowPolicy;
import com.github.devmix.process.acto.core.utils.ObjectInfoUtils;
import com.github.devmix.process.acto.exceptions.DependencyException;
import com.github.devmix.process.acto.exceptions.DispatchingException;
import com.github.devmix.process.acto.exceptions.ObjectStopException;
import com.github.devmix.process.acto.exceptions.QueueOverflowException;
import com.github.devmix.process.acto.exceptions.RegistryException;
import com.github.devmix.process.acto.listeners.OnCreateListener;
import com.github.devmix.process.acto.listeners.OnDestroyListener;
import com.github.devmix.process.acto.listeners.OnIdleListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;
import com.github.devmix.process.acto.listeners.OnStartListener;
import com.github.devmix.process.acto.listeners.OnStopListener;
import com.github.devmix.process.acto.messages.AttributeGetter;
import com.github.devmix.process.acto.messages.AttributeSetter;
import com.github.devmix.process.acto.messages.Idle;
import com.github.devmix.process.acto.messages.Invoke;
import com.github.devmix.process.acto.messages.InvokeAndGet;
import com.github.devmix.process.acto.messages.Start;
import com.github.devmix.process.acto.messages.Stop;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * @author Sergey Grachev
 */
public class DefaultActiveObjectContext<T> implements ActiveObjectContext<T>, ActiveObject<T> {

    public static final int NO_DEACTIVATION_TIMEOUT = -2;

    /**
     * @deprecated use {@code DispatcherConfig.debugLifecycleLogging} or the SLF4J DEBUG level
     */
    @Deprecated
    public static boolean DEBUGGING_INFO = false;

    private static final Logger LOG = LoggerFactory.getLogger(DefaultActiveObjectContext.class);

    private final Object id;
    private final ActiveObjectLifecycle<T> lifecycle;
    private final @Getter Object options;
    private final ActiveObjectsDispatcher dispatcher;
    private final DispatcherConfig config;
    private final Executor executor;
    private final StartGraph startGraph;
    private final AtomicBoolean queueTaskRunning = new AtomicBoolean();
    private final AtomicInteger pendingMessages = new AtomicInteger();
    private final AtomicLong rejectedMessages = new AtomicLong();
    private volatile Thread dispatchThread;
    private final LinkedBlockingDeque<QueueEntry<?>> queue = new LinkedBlockingDeque<>();

    private volatile Map<Object, DependencyState> dependencies = Map.of();

    private volatile @Getter T instance;
    private volatile @Getter ActiveObjectStatus status = ActiveObjectStatus.CREATING;
    private volatile long lastActivityTime;

    private volatile @Getter boolean idleAllowed;
    private volatile @Getter long idleTimeout;
    private volatile @Getter long waitMessagesTimeout;

    public DefaultActiveObjectContext(final Object id, final Object options, @Nullable final T instance,
                                      final ActiveObjectLifecycle<T> lifecycle,
                                      final ActiveObjectsDispatcher dispatcher, final Executor executor,
                                      final DispatcherConfig config, final StartGraph startGraph) {
        this.id = id;
        this.lifecycle = lifecycle;
        this.options = options;
        this.dispatcher = dispatcher;
        this.executor = executor;
        this.config = config;
        this.startGraph = startGraph;
        this.instance = instance;

        this.setIdleTimeout(config.getIdleTimeout().toNanos());
        this.setWaitMessagesTimeout(config.getWaitMessagesTimeout().toMillis());

        this.onCreate();
    }

    @Override
    public ActiveObjectDependency addDependsOn(final Object objectId, final boolean optional) {
        return addDependency(objectId, ActiveObjectDependency.Type.DEPENDS_ON, optional);
    }

    @Override
    public void awaitEmptyQueue(final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
        final var deadlineNanos = System.nanoTime() + unit.toNanos(timeout);
        while (pendingMessages.get() != 0) {
            if (System.nanoTime() >= deadlineNanos) {
                throw new TimeoutException("Queue is not empty: " + this);
            }
            //noinspection BusyWait
            Thread.sleep(1);
        }
    }

    @Override
    public ActiveObjectDependency getDependsOn(final Object objectId) {
        return Objects.requireNonNull(findDependsOn(objectId));
    }

    @Override
    public @Nullable ActiveObjectDependency findDependsOn(final Object objectId) {
        final var result = dependencies.get(objectId);
        return result != null && result.getType() == ActiveObjectDependency.Type.DEPENDS_ON ? result : null;
    }

    @Override
    public @Nullable ActiveObjectDependency findRequiredFor(final Object objectId) {
        final var result = dependencies.get(objectId);
        return result != null && result.getType() == ActiveObjectDependency.Type.REQUIRED_FOR ? result : null;
    }

    @Override
    public void forEachDependency(final Consumer<ActiveObjectDependency> action, @Nullable final ActiveObjectDependency.Type filterByType) {
        if (!dependencies.isEmpty()) {
            for (final var state : dependencies.values()) {
                if (filterByType == null || state.getType() == filterByType) {
                    action.accept(state);
                }
            }
        }
    }

    @Override
    public <R> CompletableFuture<R> get(final AttributeGetter<T, R> action) {
        if (action == null) {
            throw new DispatchingException("Getter function cannot be null");
        }
        return this.dispatch(action, true);
    }

    @Override
    public Object getId() {
        return id;
    }

    public void heartbeat() {
        // fast check
        boolean mustBeSend = false;
        if (idleAllowed) {
            final var now = System.nanoTime();
            if (now - lastActivityTime > idleTimeout) {
                mustBeSend = true;
            }
        }

        if (mustBeSend) {
            dispatch(InternalHeartbeat.standard());
        }
    }

    @Override
    public <R> CompletableFuture<R> redirectRequest(final Object objectId, final Object message) throws RegistryException {
        if (id.equals(objectId)) {
            return dispatch(message, true);
        }

        final var recipient = dispatcher.getById(objectId);

        return recipient.request(message);
    }

    @Override
    public void redirectRequestAndForget(final Object objectId, final Object message) throws RegistryException {
        if (id.equals(objectId)) {
            dispatchNoResponse(message, true);
        } else {
            dispatcher.getById(objectId).requestAndForget(message);
        }
    }

    @Override
    public <R> CompletableFuture<R> request(final Object message) {
        if (message == null) {
            throw new DispatchingException("Message cannot be null");
        }
        return dispatch(message, true);
    }

    @Override
    public CompletableFuture<Void> invoke(final Invoke<T> invoke) {
        if (invoke == null) {
            throw new DispatchingException("invoke cannot be null");
        }
        return dispatch(invoke, true);
    }

    @Override
    public void invokeAnForget(final Invoke<T> invoke) {
        if (invoke == null) {
            throw new DispatchingException("invoke cannot be null");
        }
        addToQueue(new QueueEntry<>(invoke, null), true);
    }

    @Override
    public void requestAndForget(final Object message) {
        if (message == null) {
            throw new DispatchingException("Message cannot be null");
        }
        addToQueue(new QueueEntry<>(message, null), true);
    }

    @Override
    public <V> CompletableFuture<Void> set(final AttributeSetter<T, V> action, @Nullable final V value) {
        if (action == null) {
            throw new DispatchingException("Setter action cannot be null");
        }
        return dispatch(InternalSetAttribute.create(action, value), true);
    }

    @Override
    public <V> void setAndForget(final AttributeSetter<T, V> action, @Nullable final V value) {
        if (action == null) {
            throw new DispatchingException("Setter action cannot be null");
        }
        dispatchNoResponse(InternalSetAttribute.create(action, value), true);
    }

    public void setIdleTimeout(final long idleTimeout) {
        this.idleTimeout = idleTimeout == -1 ? config.getIdleTimeout().toNanos() : idleTimeout;
        this.idleAllowed = this.idleTimeout != NO_DEACTIVATION_TIMEOUT
                && (instance instanceof OnIdleListener || lifecycle instanceof OnIdleListener);
    }

    @Override
    public void setInstance(@Nullable final T instance) {
        this.instance = instance;
    }

    @Override
    public void setWaitMessagesTimeout(final long waitMessagesTimeout) {
        this.waitMessagesTimeout = waitMessagesTimeout == -1 ? config.getWaitMessagesTimeout().toMillis() : waitMessagesTimeout;
    }

    @Override
    public long getRejectedMessages() {
        return rejectedMessages.get();
    }

    @Override
    public String toString() {
        return "ActiveObjectContext{" +
                "id=" + id +
                ", status=" + status +
                ", queue=" + queue.size() +
                ", running=" + queueTaskRunning.get() +
                ", deps=" + dependencies.size() +
                '}';
    }

    private DependencyState addDependency(final Object objectId, final ActiveObjectDependency.Type type, final boolean optional) {
        final var result = new DependencyState(type, optional);

        mutableDependencies().put(objectId, result);

        return result;
    }

    /**
     * The dependency map is allocated lazily: objects without dependencies share the immutable
     * empty map instance, so the per-object overhead is zero until the first dependency is added.
     */
    private synchronized Map<Object, DependencyState> mutableDependencies() {
        if (!(dependencies instanceof ConcurrentHashMap<?, ?>)) {
            dependencies = new ConcurrentHashMap<>();
        }
        @SuppressWarnings("unchecked")
        final var result = (Map<Object, DependencyState>) dependencies;
        return result;
    }

    private <R> void addToQueue(final QueueEntry<R> entry) {
        addToQueue(entry, false);
    }

    private <R> void addToQueue(final QueueEntry<R> entry, final boolean userMessage) {
        final var message = entry.message();
        if (status == ActiveObjectStatus.DESTROYED && !(message instanceof InternalDestroy)) {
            entry.fail(new DispatchingException("Cannot dispatch message [" + message + "]: object [" + id + "] is destroyed"));
            return;
        }

        // a message sent by the object to itself is internal control flow, not external load:
        // applying the policy to it would deadlock the dispatch thread under BLOCK
        if (userMessage && !isSelfDispatch() && isUserMessage(message) && config.getQueueCapacity() > 0 && queue.size() >= config.getQueueCapacity()) {
            if (config.getQueueOverflowPolicy() == QueueOverflowPolicy.BLOCK) {
                // soft limit: concurrent producers may overshoot the capacity by a few messages,
                // but the queue can never grow unboundedly
                while (queue.size() >= config.getQueueCapacity()) {
                    try {
                        Thread.sleep(1);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        final var interrupted = new DispatchingException("Interrupted while waiting for a queue slot: [" + message + "]");
                        if (entry.future() != null) {
                            entry.fail(interrupted);
                        } else {
                            throw interrupted;
                        }
                        return;
                    }
                }
            } else {
                rejectedMessages.incrementAndGet();
                final var overflow = new QueueOverflowException(
                        "Queue of object [" + id + "] is full: capacity [" + config.getQueueCapacity() + "], message [" + message + "] rejected");
                if (entry.future() != null) {
                    entry.fail(overflow);
                } else {
                    throw overflow;
                }
                return;
            }
        }

        pendingMessages.incrementAndGet();
        if ((message instanceof Stop stop && stop.isForce())
                || (message instanceof InternalDestroy destroy && destroy.isForce())) {

            // priority insertion into the head of the queue: the running dispatch task
            // (if any) will process it right after the current message.
            // Cancelling the running task is impossible (ForkJoinTask.cancel does not
            // interrupt exec()) and would break the single-consumer invariant.
            queue.offerFirst(entry);
        } else {
            // a graceful InternalDestroy goes to the tail: everything accepted before the
            // destroy request is processed first, so no accepted work is lost
            queue.offerLast(entry);
        }
        processNextMessages();
    }

    /**
     * Lifecycle and system messages are never subject to the queue overflow policy: a full
     * queue must not prevent an object from being started, stopped or destroyed.
     */
    private static boolean isUserMessage(final Object message) {
        return !(message instanceof InternalDestroy)
                && !(message instanceof InternalHeartbeat)
                && !(message instanceof InternalUpstreamDependencyStopped)
                && !(message instanceof Start)
                && !(message instanceof Stop)
                && !(message instanceof Idle.Activate)
                && !(message instanceof Idle.Deactivate);
    }

    private <R> CompletableFuture<R> dispatch(final Object message) {
        return dispatch(message, false);
    }

    private <R> CompletableFuture<R> dispatch(final Object message, final boolean userMessage) {
        final var dispatcherMessage = new QueueEntry<R>(message, new CompletableFuture<>());
        if (userMessage && isUserMessage(message) && !config.getMessageTimeout().isZero()) {
            dispatcherMessage.future().orTimeout(config.getMessageTimeout().toMillis(), TimeUnit.MILLISECONDS);
        }
        addToQueue(dispatcherMessage, userMessage);
        return dispatcherMessage.future();
    }

    private void dispatchNoResponse(final Object message) {
        dispatchNoResponse(message, false);
    }

    private void dispatchNoResponse(final Object message, final boolean userMessage) {
        addToQueue(new QueueEntry<>(message, null), userMessage);
    }

    private void notifyDependency(final Object message, final DependencyState dependency, final Object dependencyId) {
        try {
            dependency.getObject().request(message);
        } catch (final Exception e) {
            dependency.setLastError(e.getMessage());
            LOG.warn("Cannot notify dependency: {}", dependencyId, e);
        }
    }

    private void notifyStoppedDependsOnDependencies(final Stop message) {
        if (!dependencies.isEmpty()) {
            for (final var entry : dependencies.entrySet()) {
                final var dependency = entry.getValue();
                if (dependency.isResolved() && dependency.getType() == ActiveObjectDependency.Type.DEPENDS_ON) {
                    if (message.isVisited(entry.getKey())) {
                        dependency.setObject(null);
                        dependency.setLastError(null);
                    } else {
                        notifyDependency(InternalUpstreamDependencyStopped.source(this), dependency, entry.getKey());
                    }
                }
            }
        }
    }

    private void onActivate(final Idle.Activate message) {
        if (!idleAllowed) {
            return;
        }

        lifecycleLog("ACTIVATE ⟶ ⟶ [{}]", ObjectInfoUtils.printObjectState(id, dependencies, status));

        final var oldStatus = status;
        try {
            status = ActiveObjectStatus.ACTIVATING;

            if (instance instanceof OnIdleListener l && l.onObjectActivate(message, this)) {
                status = ActiveObjectStatus.ACTIVATED;
            } else if (lifecycle instanceof OnIdleListener l && l.onObjectActivate(message, this)) {
                status = ActiveObjectStatus.ACTIVATED;
            } else {
                status = oldStatus;
            }
        } catch (final Exception e) {
            status = oldStatus;
            throw e;
        } finally {
            lifecycleLog("⟵ ⟵ ACTIVATE [{}]", ObjectInfoUtils.printObjectState(id, dependencies, status));
        }
    }

    private void onCreate() {
        lifecycleLog("CREATE ⟶ ⟶ [{}]", ObjectInfoUtils.printObjectState(id, dependencies, status));

        final var oldStatus = status;
        try {
            status = ActiveObjectStatus.CREATING;

            if (instance instanceof OnCreateListener l) {
                l.onObjectCreate(this);
            } else if (lifecycle instanceof OnCreateListener l) {
                l.onObjectCreate(this);
            }

            status = ActiveObjectStatus.CREATED;
        } catch (final Exception e) {
            status = oldStatus;
            throw e;
        } finally {
            lifecycleLog("⟵ ⟵ CREATE [{}]", ObjectInfoUtils.printObjectState(id, dependencies, status));
        }
    }

    private void onDeactivate(final Idle.Deactivate message) {
        if (!idleAllowed) {
            return;
        }

        final var force = message.isForce();
        final var now = System.nanoTime();
        if (force || now - lastActivityTime > idleTimeout) {
            final var oldStatus = status;
            try {
                lifecycleLog("DEACTIVATE ⟶ ⟶ F:{} T:{} [{}]", force, TimeUnit.NANOSECONDS.toMillis(now - lastActivityTime), ObjectInfoUtils.printObjectState(id, dependencies, status));

                status = ActiveObjectStatus.DEACTIVATING;

                if (instance instanceof OnIdleListener l && l.onObjectDeactivate(message, this)) {
                    status = ActiveObjectStatus.DEACTIVATED;
                } else if (lifecycle instanceof OnIdleListener l && l.onObjectDeactivate(message, this)) {
                    status = ActiveObjectStatus.DEACTIVATED;
                } else {
                    status = oldStatus;
                }
            } catch (final Exception e) {
                if (force) {
                    status = ActiveObjectStatus.DEACTIVATED;
                } else {
                    status = oldStatus;
                    throw e;
                }
            } finally {
                lifecycleLog("⟵ ⟵ DEACTIVATE F:{}, [{}]", force, ObjectInfoUtils.printObjectState(id, dependencies, status));
            }
        }
    }

    private void onDestroy(final InternalDestroy message) {
        lifecycleLog("DESTROY ⟶ ⟶ [{}]", ObjectInfoUtils.printObjectState(id, dependencies, status));

        final var oldStatus = status;
        try {
            status = ActiveObjectStatus.DESTROYING;

            if (instance instanceof OnDestroyListener l) {
                l.onObjectDestroy(this);
            } else if (lifecycle instanceof OnDestroyListener l) {
                l.onObjectDestroy(this);
            }

            if (!dependencies.isEmpty()) {
                for (final var dependency : dependencies.values()) {
                    dependency.setObject(null);
                }
            }

            status = ActiveObjectStatus.DESTROYED;
        } catch (final Exception e) {
            status = oldStatus;
            throw e;
        } finally {
            lifecycleLog("⟵ ⟵ DESTROY [{}]", ObjectInfoUtils.printObjectState(id, dependencies, status));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object onMessage(final Object message) throws Exception {
        try {
            if (message instanceof AttributeGetter action) {
                return action.get(instance);
            } else if (message instanceof InvokeAndGet m) {
                return m.run(instance);
            } else if (instance instanceof OnMessageListener listener) {
                return listener.onObjectMessage(message, this);
            } else if (lifecycle instanceof OnMessageListener listener) {
                return listener.onObjectMessage(message, this);
            } else if (message instanceof Invoke m) {
                m.run(instance);
                return null;
            } else if (message instanceof InternalSetAttribute m) {
                m.getAction().set(instance, m.getValue());
                return null;
            }
        } finally {
            updateLastActivityTime();
        }
        throw new DispatchingException("Unsupported message: " + message);
    }

    private Object onStart(final Start message) {
        lifecycleLog("START ⟶ ⟶ C:{} [{}]", ObjectInfoUtils.unrollChain(message.getChain()), ObjectInfoUtils.printObjectState(id, dependencies, status));

        final var oldStatus = status;
        try {
            status = ActiveObjectStatus.STARTING;

            message.starting(this);

            startDependsOnDependencies(message);

            if (instance instanceof OnStartListener l) {
                l.onObjectStart(message, this);
            } else if (lifecycle instanceof OnStartListener l) {
                l.onObjectStart(message, this);
            }

            final var source = message.getSource();
            if (source != null) {
                addDependency(source.getId(), ActiveObjectDependency.Type.REQUIRED_FOR, message.isOptional()).setObject(source);
            }

//            startDynamicDependencies();

            status = ActiveObjectStatus.STARTED;
        } catch (final Exception e) {
            status = oldStatus;
            throw e;
        } finally {
            lifecycleLog("⟵ ⟵ START C:{} [{}]", ObjectInfoUtils.unrollChain(message.getChain()), ObjectInfoUtils.printObjectState(id, dependencies, status));
        }

        return this;
    }

    private Boolean onStop(final Stop message) {
        lifecycleLog("STOP ⟶ ⟶ C:{} [{}]", ObjectInfoUtils.unrollChain(message.getChain()), ObjectInfoUtils.printObjectState(id, dependencies, status));

        final var force = message.isForce();

        final var source = message.getSource();
        if (source != null && !force) {
            return Boolean.FALSE;
        }

        final var oldStatus = status;
        try {
            status = ActiveObjectStatus.STOPPING;

            message.stopping(this);

            stopRequiredForDependencies(message);

            if (instance instanceof OnStopListener l) {
                l.onObjectStop(message, this);
            } else if (lifecycle instanceof OnStopListener l) {
                l.onObjectStop(message, this);
            }

            notifyStoppedDependsOnDependencies(message);

            status = ActiveObjectStatus.STOPPED;
        } catch (final Exception e) {
            if (force) {
                status = ActiveObjectStatus.STOPPED;
            } else {
                status = oldStatus;
                throw e;
            }
        } finally {
            lifecycleLog("⟵ ⟵ STOP C:{} [{}]", ObjectInfoUtils.unrollChain(message.getChain()), ObjectInfoUtils.printObjectState(id, dependencies, status));
        }

        return Boolean.TRUE;
    }

    private void onUpstreamDependencyStopped(final InternalUpstreamDependencyStopped message) {
        lifecycleLog("REMOVE UPSTREAM DEP ⟶ ⟶ ID:{} [{}]", message.getSource().getId(), ObjectInfoUtils.printObjectState(id, dependencies, status));

        if (!dependencies.isEmpty()) {
            final var dependencyId = message.getSource().getId();
            final var dependency = dependencies.get(dependencyId);
            if (dependency != null) {
                if (dependency.getType() != ActiveObjectDependency.Type.REQUIRED_FOR) {
                    //TODO SG exception
                } else {
                    mutableDependencies().remove(dependencyId);
                }
            }
        }

        lifecycleLog("⟵ ⟵ REMOVED UPSTREAM DEP ID:{} [{}]", message.getSource().getId(), ObjectInfoUtils.printObjectState(id, dependencies, status));
    }

    private Object processNextMessage(final Object message) throws Exception {
        if (status.isTransient()) {
            throw new DispatchingException("Illegal state for the message processing: " + status);
        }

        // system messages
        if (message instanceof InternalUpstreamDependencyStopped m) {
            onUpstreamDependencyStopped(m);
            return null;
        }

        // lifecycle & user messages
        switch (status) {
            case CREATED:
                if (message instanceof InternalHeartbeat) {
                    // ignore
                    return null;
                } else if (message instanceof Start m) {
                    return onStart(m);
                } else if (message instanceof Idle.Activate) {
                    final var object = onStart(Start.standard());
                    onActivate(Idle.activate());
                    return object;
                } else if (message instanceof Idle.Deactivate) {
                    // ignore
                    return null;
                } else if (message instanceof Stop m) {
                    return onStop(m);
                } else if (message instanceof InternalDestroy m) {
                    onDestroy(m);
                    return Boolean.TRUE;
                }

                onStart(Start.standard());
                onActivate(Idle.activate());
                return onMessage(message);

            case STARTED:
            case DEACTIVATED:
                if (message instanceof InternalHeartbeat) {
                    // ignore
                    return null;
                } else if (message instanceof Start) {
                    // ignore: already started, return the active object itself
                    return this;
                } else if (message instanceof Idle.Activate) {
                    onActivate(Idle.activate());
                    return null;
                } else if (message instanceof Idle.Deactivate) {
                    // ignore
                    return null;
                } else if (message instanceof Stop m) {
                    return onStop(m);
                } else if (message instanceof InternalDestroy m) {
                    if (status != ActiveObjectStatus.DEACTIVATED) {
                        onDeactivate(Idle.deactivateNow());
                    }
                    onStop(Stop.force());
                    onDestroy(m);
                    return Boolean.TRUE;
                }

                onActivate(Idle.activate());
                return onMessage(message);

            case ACTIVATED:
                if (message instanceof InternalHeartbeat) {
                    onDeactivate(Idle.deactivate());
                    return null;
                } else if (message instanceof Start) {
                    // ignore: already started, return the active object itself
                    return this;
                } else if (message instanceof Idle.Activate) {
                    // ignore
                    return null;
                } else if (message instanceof Idle.Deactivate) {
                    onDeactivate(Idle.deactivate());
                    return null;
                } else if (message instanceof Stop m) {
                    return onStop(m);
                } else if (message instanceof InternalDestroy m) {
                    onDeactivate(Idle.deactivateNow());
                    onStop(Stop.force());
                    onDestroy(m);
                    return Boolean.TRUE;
                }

                return onMessage(message);

            case STOPPED:
                if (message instanceof InternalHeartbeat) {
                    // ignore
                    return null;
                } else if (message instanceof Start m) {
                    return onStart(m);
                } else if (message instanceof Stop) {
                    // ignore
                    return Boolean.TRUE;
                } else if (message instanceof InternalDestroy m) {
                    onDestroy(m);
                    return Boolean.TRUE;
                }

                // User message || ActivateMessage || DeactivateMessage -> exception
                break;

            case DESTROYED:
                if (message instanceof InternalDestroy) {
                    // ignore: already destroyed, destroy is idempotent
                    return Boolean.TRUE;
                }
                // -> exception

            default:
                // ignore
        }
        throw new DispatchingException("Cannot dispatch message [" + message + "] in the status = [" + status + "]");
    }

    private void processNextMessages() {
        if (!queueTaskRunning.compareAndSet(false, true)) {
            return;
        }

        executor.execute(new QueueDispatchingTask());
    }

    /**
     * A message sent by the object to itself is internal control flow, not external load:
     * applying the overflow policy to it would block the dispatch thread waiting for a slot
     * that only it can free. Such messages bypass the policy.
     */
    private boolean isSelfDispatch() {
        return Thread.currentThread() == dispatchThread;
    }

    private void startDependency(final Start message, final DependencyState dependency, final Object dependencyId) {
        if (startGraph.reaches(dependencyId, id)) {
            throw new DependencyException("Cyclic start: " + id + " -> " + dependencyId);
        }

        startGraph.add(id, dependencyId);
        try {
            dependency.setObject(this.<ActiveObject<?>>redirectRequest(dependencyId, message)
                    .get(config.getDependencyStartTimeout().toMillis(), TimeUnit.MILLISECONDS));
            dependency.setLastError(null);
        } catch (final Exception e) {
            dependency.setLastError(e.getMessage());
            if (!dependency.isOptional()) {
                throw new DependencyException("Cannot start dependency: " + dependencyId, e);
            } else {
                LOG.warn("Cannot start optional dependency: {}", dependencyId, e);
            }
        } finally {
            startGraph.remove(id, dependencyId);
        }
    }

    private void startDependsOnDependencies(final Start message) {
        if (!dependencies.isEmpty()) {
            final var entries = new ArrayList<>(dependencies.entrySet());
            entries.sort(Comparator.comparing(entry -> String.valueOf(entry.getKey())));
            for (final var entry : entries) {
                final var dependency = entry.getValue();
                if (dependency.getType() == ActiveObjectDependency.Type.DEPENDS_ON) {
                    final var dependencyId = entry.getKey();
                    if (message.isVisited(dependencyId)) {
                        throw new DependencyException("Cyclic dependency: " + ObjectInfoUtils.unrollChain(message.getChain()));
                    }

                    startDependency(Start.downstream(this, message, dependency.isOptional()), dependency, dependencyId);
                }
            }
        }
    }

    private boolean stopDependency(final Stop message, final DependencyState dependency, final Object dependencyId) {
        try {
            final var success = Boolean.TRUE.equals(dependency.getObject()
                    .request(message).get(config.getDependencyStopTimeout().toMillis(), TimeUnit.MILLISECONDS));
            dependency.setLastError(null);
            dependency.setObject(null);
            return success;
        } catch (final Exception e) {
            dependency.setLastError(e.getMessage());
            throw new DependencyException("Cannot stop dependency: " + dependencyId);
        }
    }

    private void stopRequiredForDependencies(final Stop message) {
        if (!dependencies.isEmpty()) {
            for (final var entry : new ArrayList<>(dependencies.entrySet())) {
                final var dependency = entry.getValue();
                if (dependency.isResolved() && dependency.getType() == ActiveObjectDependency.Type.REQUIRED_FOR) {
                    final var dependencyId = entry.getKey();
                    final var upstreamMsg = Stop.upstream(this, message);
                    if (!stopDependency(upstreamMsg, dependency, dependencyId)) {
                        throw new ObjectStopException("Object [" + id + "] cannot be stopped because blocked by [" + dependencyId + "]", dependencyId);
                    }
                    mutableDependencies().remove(dependencyId);
                }
            }
        }
    }

    private void updateLastActivityTime() {
        lastActivityTime = System.nanoTime();
    }

    private void lifecycleLog(final String format, final Object... args) {
        if (config.isDebugLifecycleLogging() || DEBUGGING_INFO) {
            LOG.info(format, args);
        } else {
            LOG.debug(format, args);
        }
    }

    private void drainQueueAfterDestroy() {
        var drained = 0;
        QueueEntry<?> entry;
        while ((entry = queue.poll()) != null) {
            entry.fail(new DispatchingException("Cannot dispatch message [" + entry.message() + "]: object [" + id + "] is destroyed"));
            pendingMessages.decrementAndGet();
            drained++;
        }
        if (drained > 0) {
            LOG.debug("Drained {} queued message(s) of destroyed object [{}]", drained, id);
        }
    }

    private final class QueueDispatchingTask implements Runnable {
        @Override
        public void run() {
            dispatchThread = Thread.currentThread();
            try {
                while (queueTaskRunning.get()) {
                    if (status == ActiveObjectStatus.DESTROYED) {
                        drainQueueAfterDestroy();
                        break;
                    }
                    var msg = queue.poll();
                    if (msg == null) {
                        if (waitMessagesTimeout > 0) {
                            try {
                                msg = queue.poll(waitMessagesTimeout, TimeUnit.MILLISECONDS);
                                if (msg == null) {
                                    break;
                                }
                            } catch (final InterruptedException e) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    try {
                        if (msg.future() != null && msg.future().isCompletedExceptionally()) {
                            // the caller timed out or cancelled this request: skip the processing
                            continue;
                        }
                        msg.success(processNextMessage(msg.message()));
                    } catch (final Exception e) {
                        LOG.error(e.getMessage(), e);
                        msg.fail(e);
                    } finally {
                        pendingMessages.decrementAndGet();
                    }
                }
            } finally {
                dispatchThread = null;
                queueTaskRunning.set(false);
                if (!queue.isEmpty()) {
                    processNextMessages();
                }
            }
        }
    }
}
