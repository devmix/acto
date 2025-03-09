package com.github.devmix.process.acto.core.context;

import com.github.devmix.process.acto.ActiveObject;
import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.ActiveObjectDependency;
import com.github.devmix.process.acto.ActiveObjectLifecycle;
import com.github.devmix.process.acto.ActiveObjectStatus;
import com.github.devmix.process.acto.ActiveObjectsDispatcher;
import com.github.devmix.process.acto.core.utils.ObjectInfoUtils;
import com.github.devmix.process.acto.exceptions.DependencyException;
import com.github.devmix.process.acto.exceptions.DispatchingException;
import com.github.devmix.process.acto.exceptions.ObjectStopException;
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
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author Sergey Grachev
 */
public class DefaultActiveObjectContext<T> implements ActiveObjectContext<T>, ActiveObject<T> {

    public static final int NO_DEACTIVATION_TIMEOUT = -2;
    public static boolean DEBUGGING_INFO = true;

    private static final Logger LOG = LoggerFactory.getLogger(DefaultActiveObjectContext.class);

    private final Object id;
    private final ActiveObjectLifecycle<T> lifecycle;
    private final @Getter Object options;
    private final ActiveObjectsDispatcher dispatcher;
    private final ForkJoinPool executor;
    //    private final AtomicLong numberOfThreads = new AtomicLong(0);
    private final AtomicBoolean queueTaskRunning = new AtomicBoolean();
    private final LinkedBlockingDeque<QueueEntry<?>> queue = new LinkedBlockingDeque<>();
    private final AtomicReference<QueueDispatchingTask> dispatchingTask = new AtomicReference<>();

    private Map<Object, DependencyState> dependencies;

    private @Getter T instance;
    private @Getter ActiveObjectStatus status = ActiveObjectStatus.CREATING;
    private long lastActivityTime;

    private @Getter boolean idleAllowed;
    private @Getter long idleTimeout;
    private @Getter long waitMessagesTimeout;

    public DefaultActiveObjectContext(final Object id, final Object options, @Nullable final T instance,
                                      final ActiveObjectLifecycle<T> lifecycle,
                                      final ActiveObjectsDispatcher dispatcher, final ForkJoinPool executor) {
        this.id = id;
        this.lifecycle = lifecycle;
        this.options = options;
        this.dispatcher = dispatcher;
        this.executor = executor;
        this.instance = instance;

        this.setIdleTimeout(dispatcher.getIdleTimeout());
        this.setWaitMessagesTimeout(dispatcher.getWaitMessagesTimeout());

        this.onCreate();
    }

    @Override
    public ActiveObjectDependency addDependsOn(final Object objectId, final boolean optional) {
        return addDependency(objectId, ActiveObjectDependency.Type.DEPENDS_ON, optional);
    }

    @Override
    public void awaitEmptyQueue(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (queueTaskRunning.compareAndSet(true, false)) {
            final var task = dispatchingTask.get();
            if (task != null) {
                task.get(timeout, unit);
            }
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
        if (MapUtils.isNotEmpty(dependencies)) {
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
        return this.dispatch(action);
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
            return dispatch(message);
        }

        final var recipient = dispatcher.getById(objectId);

        return recipient.request(message);
    }

    @Override
    public void redirectRequestAndForget(final Object objectId, final Object message) throws RegistryException {
        if (id.equals(objectId)) {
            dispatchNoResponse(message);
        } else {
            dispatcher.getById(objectId).requestAndForget(message);
        }
    }

    @Override
    public <R> CompletableFuture<R> request(final Object message) {
        if (message == null) {
            throw new DispatchingException("Message cannot be null");
        }
        return dispatch(message);
    }

    @Override
    public CompletableFuture<Void> invoke(final Invoke<T> invoke) {
        if (invoke == null) {
            throw new DispatchingException("invoke cannot be null");
        }
        return dispatch(invoke);
    }

    @Override
    public void invokeAnForget(final Invoke<T> invoke) {
        if (invoke == null) {
            throw new DispatchingException("invoke cannot be null");
        }
        addToQueue(new QueueEntry<>(invoke, null));
    }

    @Override
    public void requestAndForget(final Object message) {
        if (message == null) {
            throw new DispatchingException("Message cannot be null");
        }
        addToQueue(new QueueEntry<>(message, null));
    }

    @Override
    public <V> CompletableFuture<Void> set(final AttributeSetter<T, V> action, @Nullable final V value) {
        if (action == null) {
            throw new DispatchingException("Setter action cannot be null");
        }
        return dispatch(InternalSetAttribute.create(action, value));
    }

    @Override
    public <V> void setAndForget(final AttributeSetter<T, V> action, @Nullable final V value) {
        if (action == null) {
            throw new DispatchingException("Setter action cannot be null");
        }
        dispatchNoResponse(InternalSetAttribute.create(action, value));
    }

    public void setIdleTimeout(final long idleTimeout) {
        this.idleTimeout = idleTimeout == -1 ? dispatcher.getIdleTimeout() : idleTimeout;
        this.idleAllowed = this.idleTimeout != NO_DEACTIVATION_TIMEOUT
                && (instance instanceof OnIdleListener || lifecycle instanceof OnIdleListener);
    }

    @Override
    public void setInstance(@Nullable final T instance) {
        this.instance = instance;
    }

    @Override
    public void setWaitMessagesTimeout(final long waitMessagesTimeout) {
        this.waitMessagesTimeout = waitMessagesTimeout == -1 ? dispatcher.getWaitMessagesTimeout() : waitMessagesTimeout;
    }

    @Override
    public String toString() {
        return "ActiveObjectContext{" +
                "id=" + id +
                ", status=" + status +
                ", queue=" + queue.size() +
                ", running=" + queueTaskRunning.get() +
                ", deps=" + (dependencies != null ? dependencies.size() : 0) +
                '}';
    }

    private DependencyState addDependency(final Object objectId, final ActiveObjectDependency.Type type, final boolean optional) {
        if (dependencies == null) {
            dependencies = new HashMap<>();
        }

        final var result = new DependencyState(type, optional);

        dependencies.put(objectId, result);

        return result;
    }

    private <R> void addToQueue(final QueueEntry<R> entry) {
        final var message = entry.message();
        if ((message instanceof Stop stop && stop.isForce())
                || message instanceof InternalDestroy) {

            if (queueTaskRunning.compareAndSet(true, false)) {
                final var task = dispatchingTask.get();
                if (task != null) {
                    task.cancel(true);
                }
            }

            queue.offerFirst(entry);
        } else {
            queue.offerLast(entry);
        }
        processNextMessages();
    }

    private <R> CompletableFuture<R> dispatch(final Object message) {
        final var dispatcherMessage = new QueueEntry<>(message, new CompletableFuture<R>());
        addToQueue(dispatcherMessage);
        return dispatcherMessage.future();
    }

    private void dispatchNoResponse(final Object message) {
        addToQueue(new QueueEntry<>(message, null));
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
        if (MapUtils.isNotEmpty(dependencies)) {
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

        if (DEBUGGING_INFO) {
            LOG.info("ACTIVATE ⟶ ⟶ [{}]", ObjectInfoUtils.printObjectState(id, dependencies, status));
        }

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
            if (DEBUGGING_INFO) {
                LOG.info("⟵ ⟵ ACTIVATE [{}]", ObjectInfoUtils.printObjectState(id, dependencies, status));
            }
        }
    }

    private void onCreate() {
        if (DEBUGGING_INFO) {
            LOG.info("CREATE ⟶ ⟶ [{}]", ObjectInfoUtils.printObjectState(id, dependencies, status));
        }

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
            if (DEBUGGING_INFO) {
                LOG.info("⟵ ⟵ CREATE [{}]", ObjectInfoUtils.printObjectState(id, dependencies, status));
            }
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
                if (DEBUGGING_INFO) {
                    LOG.info("DEACTIVATE ⟶ ⟶ F:{} T:{} [{}]",
                            force, TimeUnit.NANOSECONDS.toMillis(now - lastActivityTime),
                            ObjectInfoUtils.printObjectState(id, dependencies, status));
                }

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
                if (DEBUGGING_INFO) {
                    LOG.info("⟵ ⟵ DEACTIVATE F:{}, [{}]",
                            force, ObjectInfoUtils.printObjectState(id, dependencies, status));
                }
            }
        }
    }

    private void onDestroy(final InternalDestroy message) {
        if (DEBUGGING_INFO) {
            LOG.info("DESTROY ⟶ ⟶ [{}]", ObjectInfoUtils.printObjectState(id, dependencies, status));
        }

        final var oldStatus = status;
        try {
            status = ActiveObjectStatus.DESTROYING;

            if (instance instanceof OnDestroyListener l) {
                l.onObjectDestroy(this);
            } else if (lifecycle instanceof OnDestroyListener l) {
                l.onObjectDestroy(this);
            }

            if (MapUtils.isNotEmpty(dependencies)) {
                for (final var dependency : dependencies.values()) {
                    dependency.setObject(null);
                }
            }

            status = ActiveObjectStatus.DESTROYED;
        } catch (final Exception e) {
            status = oldStatus;
            throw e;
        } finally {
            if (DEBUGGING_INFO) {
                LOG.info("⟵ ⟵ DESTROY [{}]", ObjectInfoUtils.printObjectState(id, dependencies, status));
            }
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
        if (DEBUGGING_INFO) {
            LOG.info("START ⟶ ⟶ C:{} [{}]",
                    ObjectInfoUtils.unrollChain(message.getChain()),
                    ObjectInfoUtils.printObjectState(id, dependencies, status));
        }

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
            if (DEBUGGING_INFO) {
                LOG.info("⟵ ⟵ START C:{} [{}]",
                        ObjectInfoUtils.unrollChain(message.getChain()),
                        ObjectInfoUtils.printObjectState(id, dependencies, status));
            }
        }

        return this;
    }

    private Boolean onStop(final Stop message) {
        if (DEBUGGING_INFO) {
            LOG.info("STOP ⟶ ⟶ C:{} [{}]",
                    ObjectInfoUtils.unrollChain(message.getChain()),
                    ObjectInfoUtils.printObjectState(id, dependencies, status));
        }

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
            if (DEBUGGING_INFO) {
                LOG.info("⟵ ⟵ STOP C:{} [{}]",
                        ObjectInfoUtils.unrollChain(message.getChain()),
                        ObjectInfoUtils.printObjectState(id, dependencies, status));
            }
        }

        return Boolean.TRUE;
    }

    private void onUpstreamDependencyStopped(final InternalUpstreamDependencyStopped message) {
        if (DEBUGGING_INFO) {
            LOG.info("REMOVE UPSTREAM DEP ⟶ ⟶ ID:{} [{}]",
                    message.getSource().getId(), ObjectInfoUtils.printObjectState(id, dependencies, status));
        }

        if (MapUtils.isNotEmpty(dependencies)) {
            final var dependencyId = message.getSource().getId();
            final var dependency = dependencies.get(dependencyId);
            if (dependency != null) {
                if (dependency.getType() != ActiveObjectDependency.Type.REQUIRED_FOR) {
                    //TODO SG exception
                } else {
                    dependencies.remove(dependencyId);
                }
            }
        }

        if (DEBUGGING_INFO) {
            LOG.info("⟵ ⟵ REMOVED UPSTREAM DEP ID:{} [{}]",
                    message.getSource().getId(), ObjectInfoUtils.printObjectState(id, dependencies, status));
        }
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
                    return null;
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
                    // ignore
                    return instance;
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
                    return null;
                }

                onActivate(Idle.activate());
                return onMessage(message);

            case ACTIVATED:
                if (message instanceof InternalHeartbeat) {
                    onDeactivate(Idle.deactivate());
                    return null;
                } else if (message instanceof Start) {
                    // ignore
                    return instance;
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
                    return null;
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
                    return null;
                }

                // User message || ActivateMessage || DeactivateMessage -> exception
                break;

            case DESTROYED:
                if (message instanceof InternalDestroy) {
                    // ignore
                    return null;
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

        final var task = new QueueDispatchingTask();
        executor.submit(task);
        dispatchingTask.set(task);
    }

    private void startDependency(final Start message, final DependencyState dependency, final Object dependencyId) {
        // TODO SG configurable timeout
        try {
            dependency.setObject(this.<ActiveObject<?>>redirectRequest(dependencyId, message).get(15, TimeUnit.SECONDS));
            dependency.setLastError(null);
        } catch (final Exception e) {
            dependency.setLastError(e.getMessage());
            if (!dependency.isOptional()) {
                throw new DependencyException("Cannot start dependency: " + dependencyId, e);
            } else {
                LOG.warn("Cannot start optional dependency: {}", dependencyId, e);
            }
        }
    }

    private void startDependsOnDependencies(final Start message) {
        if (MapUtils.isNotEmpty(dependencies)) {
            for (final var entry : dependencies.entrySet()) {
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
        // TODO SG configurable timeout
        try {
            final var success = Boolean.TRUE.equals(dependency.getObject().request(message).get(15, TimeUnit.SECONDS));
            dependency.setLastError(null);
            dependency.setObject(null);
            return success;
        } catch (final Exception e) {
            dependency.setLastError(e.getMessage());
            throw new DependencyException("Cannot stop dependency: " + dependencyId);
        }
    }

    private void stopRequiredForDependencies(final Stop message) {
        if (MapUtils.isNotEmpty(dependencies)) {
            for (final var entry : new ArrayList<>(dependencies.entrySet())) {
                final var dependency = entry.getValue();
                if (dependency.isResolved() && dependency.getType() == ActiveObjectDependency.Type.REQUIRED_FOR) {
                    final var dependencyId = entry.getKey();
                    final var upstreamMsg = Stop.upstream(this, message);
                    if (!stopDependency(upstreamMsg, dependency, dependencyId)) {
                        throw new ObjectStopException("Object [" + id + "] cannot be stopped because blocked by [" + dependencyId + "]", dependencyId);
                    }
                    dependencies.remove(dependencyId);
                }
            }
        }
    }

    private void updateLastActivityTime() {
        lastActivityTime = System.nanoTime();
    }

    private final class QueueDispatchingTask extends ForkJoinTask<Void> {
        @Serial
        private static final long serialVersionUID = -1386299394796439117L;

        public boolean exec() {
            //            numberOfThreads.incrementAndGet();
            try {
                while (queueTaskRunning.get()) {
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
                        msg.success(processNextMessage(msg.message()));
                    } catch (final Exception e) {
                        LOG.error(e.getMessage(), e);
                        msg.fail(e);
                    }
                }
            } finally {
//                if (numberOfThreads.decrementAndGet() > 0) {
//                    LOG.warn("parallel threads in the dispatcher {}, {}", numberOfThreads.get(), id);
//                } else {
//                LOG.warn("stop dispatcher {}, {}", numberOfThreads.get(), id);
//                }
                dispatchingTask.set(null);
                queueTaskRunning.set(false);
                if (!queue.isEmpty()) {
                    processNextMessages();
                }
            }
            return true;
        }

        public Void getRawResult() {
            return null;
        }

        public void setRawResult(final Void v) {
        }
    }
}
