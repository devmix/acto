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

package com.github.devmix.process.acto.fixtures;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.ActiveObjectDependency;
import com.github.devmix.process.acto.fixtures.test.NodeObject;
import com.github.devmix.process.acto.listeners.OnCreateListener;
import com.github.devmix.process.acto.listeners.OnIdleListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;
import com.github.devmix.process.acto.listeners.OnStartListener;
import com.github.devmix.process.acto.messages.Idle;
import com.github.devmix.process.acto.messages.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Sergey Grachev
 */
public class EchoObject implements OnCreateListener<EchoObject>, OnStartListener<EchoObject>, OnMessageListener<NodeObject>, OnIdleListener<EchoObject> {

    private static final Logger LOG = LoggerFactory.getLogger(EchoObject.class);

    private static final AtomicLong COUNTER = new AtomicLong(0);

    public static final Integer MSG_PING = 1;
    public static final Integer MSG_BROADCAST = 2;

    private final String id;
    private final EchoOptions options;

    private ActiveObjectContext<EchoObject> context;
    private boolean allowIdle;

    public EchoObject(final String id, @Nullable final EchoOptions options) {
        this.id = id;
        this.options = options;
    }

    @Override
    public boolean onObjectActivate(final Idle.Activate message, final ActiveObjectContext<EchoObject> context) {
        return allowIdle;
    }

    @Override
    public boolean onObjectDeactivate(final Idle.Deactivate message, final ActiveObjectContext<EchoObject> context) {
        return allowIdle;
    }

    @Override
    public void onObjectCreate(final ActiveObjectContext<EchoObject> context) {
        this.context = context;

        if (options == null) {
            return;
        }

        allowIdle = options.isAllowIdle();
        context.setIdleTimeout(options.getIdleTimeout());

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
    public void onObjectStart(final Start message, final ActiveObjectContext<EchoObject> context) {
//        try {
//            Thread.sleep(100);
//        } catch (final InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    @Nullable
    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<NodeObject> context) {
        if (message == MSG_PING) {
            return "pong: " + COUNTER.incrementAndGet();
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

    public void setIdleTimeout(final long timeout) {
        this.allowIdle = timeout > 0;
        context.setIdleTimeout(timeout);
    }
}
