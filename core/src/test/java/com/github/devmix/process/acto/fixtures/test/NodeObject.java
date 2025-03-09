package com.github.devmix.process.acto.fixtures.test;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.ActiveObjectDependency;
import com.github.devmix.process.acto.listeners.OnCreateListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;
import com.github.devmix.process.acto.listeners.OnStartListener;
import com.github.devmix.process.acto.messages.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Sergey Grachev
 */
public class NodeObject implements OnCreateListener<NodeObject>, OnStartListener<NodeObject>, OnMessageListener<NodeObject> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeObject.class);

    private static final AtomicLong COUNTER = new AtomicLong(0);

    public static final Integer MSG_HELLO = 1;
    public static final Integer MSG_BROADCAST = 2;

    private final String id;
    private final NodeOptions options;
    private final ArrayList<ActiveObjectDependency> dependencies = new ArrayList<>();

    private ActiveObjectContext<NodeObject> context;

    public NodeObject(final String id, @Nullable final NodeOptions options) {
        this.id = id;
        this.options = options;
    }

    @Override
    public void onObjectCreate(final ActiveObjectContext<NodeObject> context) {
        this.context = context;
        if (options == null) {
            return;
        }

        final var dependsOn = options.getDependsOn();
        if (dependsOn != null) {
            for (final var objectId : dependsOn) {
                context.addDependsOn(objectId, false);
            }
        }

        final var dependsOnOptional = options.getDependsOnOptional();
        if (dependsOnOptional != null) {
            for (final var objectId : dependsOnOptional) {
                context.addDependsOn(objectId, true);
            }
        }
    }

    @Override
    public void onObjectStart(final Start message, ActiveObjectContext<NodeObject> context) {
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public Object onObjectMessage(final Object message, ActiveObjectContext<NodeObject> context) {
        if (message == MSG_HELLO) {
//            Thread.sleep(1);
            final var result = "\nWORLD " + COUNTER.incrementAndGet() + "\n";
//            LOG.debug("{}, {}", Thread.currentThread().getName(), result);
//            System.out.println(result);
            return result;
        } else if (message == MSG_BROADCAST) {
            System.out.println("broadcast: " + id + " - " + getCounter());
            this.context.forEachDependency(d -> {
                d.ifResolved(ref -> {
                    try {
                        ref.request(MSG_BROADCAST).get();
                    } catch (final InterruptedException | ExecutionException e) {
                        // ignore
                    }
                });
            }, ActiveObjectDependency.Type.DEPENDS_ON);
        }
        return null;
    }

    public long getCounter() {
        return COUNTER.get();
    }

    public void setString(final String value) {
        System.out.println("set " + value);
    }
}
