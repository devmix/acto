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

package com.github.devmix.acto;

import com.github.devmix.process.acto.ActiveObject;
import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.core.context.DefaultActiveObjectContext;
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher;
import com.github.devmix.process.acto.fixtures.test.HelloWorldObject;
import com.github.devmix.process.acto.fixtures.test.NodeFactory;
import com.github.devmix.process.acto.fixtures.test.NodeObject;
import com.github.devmix.process.acto.fixtures.test.NodeOptions;
import com.github.devmix.process.acto.fixtures.test.SomeFileSystemObject;
import com.github.devmix.process.acto.messages.Stop;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Sergey Grachev
 */
public class Main {

    public static final int THREADS = 10;
    public static final int MESSAGES = 100_000;

    public static void main(final String[] args) throws ExecutionException, InterruptedException, IOException, TimeoutException {
        DefaultActiveObjectContext.DEBUGGING_INFO = true;

//        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("ROOT")).setLevel(Level.TRACE);

        final var r = new DefaultActiveObjectsDispatcher();
        r.registerFactory(NodeObject.class, new NodeFactory());
        r.registerFactory(HelloWorldObject.class, HelloWorldObject::new);

        final var o1 = r.create(NodeObject.class, "n:1", new NodeOptions("data-1", new String[]{"n:2"}, null));
        final var o2 = r.create(NodeObject.class, "n:2", new NodeOptions("data-2", new String[]{"n:3"}, null));
        final var o3 = r.create(NodeObject.class, "n:3", new NodeOptions("data-3", new String[]{"n:4", "n:5"}, new String[]{"n:6"}));
        final var o4 = r.create(NodeObject.class, "n:4", new NodeOptions("data-4", null, null));
        final var o5 = r.create(NodeObject.class, "n:5", new NodeOptions("data-5", null, null));
//        final var object5 = registry.create(new SampleActiveObjectState("5", "data-5", new String[]{"sample:1"}, null));
//        final var object6 = registry.create(new SampleActiveObjectState("6", "data-6", new String[]{"sample:1"}, null));
        final var o6 = r.create(NodeObject.class, "n:6", new NodeOptions("data-6", null, null));
//        final var o7 = r.objectOf(new ClassState<>("7", "/tmp", SomeFileSystemObject.class));
//        final var o7 = r.objectOf(SomeFileSystemObject::new,
//                SomeFileSystemObject::onCreate, null, null, null, null, "1", "/tpm");
        final var o7 = r.create(SomeFileSystemObject::new, "fs:1", "/tmp");

        o7.get(SomeFileSystemObject::getFilesList).thenAccept(System.out::println);

//        System.out.println(o1.request(NodeObject.MSG_HELLO).get());
//        System.out.println(o1.request(NodeObject.MSG_HELLO).get());
//        System.out.println(o1.request(SuspendMessage.create()).get());
//        System.out.println(o1.request(NodeObject.MSG_HELLO).get());
//        System.out.println(o1.request(StopMessage.create(false)).get());
//        System.out.println(o1.request(StartMessage.create()).get());
//        System.out.println(o1.request(SuspendMessage.create()).get());

        System.out.println(o1.request(NodeObject.MSG_HELLO).get());

        // broadcast

//        o1.request(NodeObject.MSG_BROADCAST);

        o5.request(Stop.force()).get();

        o6.requestAndForget(NodeObject.MSG_HELLO);
        o6.requestAndForget(NodeObject.MSG_HELLO);
        o6.get(NodeObject::getCounter).thenAccept(response -> {
            System.out.println(response);
        });
//        o1.command(NodeObject::setString, "asd");
//        object2.message(StopMessage.create(false, false)).get();


        System.out.println(o1);
        System.out.println(o6);

//        object1.message(StartMessage.create()).get();

        load(o6);
//        Thread.sleep(2000);

//        r.shutdownAndAwaitTermination(5, TimeUnit.MINUTES);

//        ((DefaultObjectContext) o6).terminate(5, TimeUnit.MINUTES);
        System.out.println(o6.get(NodeObject::getCounter).get());
    }

    private static void load(final ActiveObject<?> object) throws InterruptedException, ExecutionException, TimeoutException {
        final var executor = Executors.newFixedThreadPool(THREADS);
        final var startSignal = new CountDownLatch(THREADS + 1);
        final var doneSignal = new CountDownLatch(1);
        for (var threadIndex = 0; threadIndex < THREADS; threadIndex++) {
            executor.submit(() -> {
                startSignal.countDown();
                try {
                    startSignal.await();
                } catch (final InterruptedException e) {
                    return;
                }

                for (var msgIndex = 0; msgIndex < MESSAGES; msgIndex++) {
//                    try {
//                        Thread.sleep(1);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
                    object.request(NodeObject.MSG_HELLO);
                }
                doneSignal.countDown();
            });
        }

        startSignal.countDown();
        startSignal.await();

        final var now = System.nanoTime();
        doneSignal.await();

        executor.shutdown();

        ((ActiveObjectContext) object).awaitEmptyQueue(5, TimeUnit.MINUTES);

        System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - now) + " msec");
    }
}
