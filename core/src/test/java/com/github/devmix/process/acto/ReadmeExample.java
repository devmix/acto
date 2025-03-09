package com.github.devmix.process.acto;

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
public class ReadmeExample {

    // Define the number of threads for concurrent testing
    public static final int THREADS = 10;

    // Define the number of messages to be sent during load test
    public static final int MESSAGES = 100_000;

    public static void main(final String[] args) throws ExecutionException, InterruptedException, IOException, TimeoutException {
        // Enable debugging information for the DefaultActiveObjectContext
        DefaultActiveObjectContext.DEBUGGING_INFO = true;

        // Create an instance of the Active Objects dispatcher
        final var r = new DefaultActiveObjectsDispatcher();

        // Register factories for NodeObject and HelloWorldObject classes
        r.registerFactory(NodeObject.class, new NodeFactory());
        r.registerFactory(HelloWorldObject.class, HelloWorldObject::new);

        // Create instances of NodeObject with specific configurations
        final var o1 = r.create(NodeObject.class, "n:1", new NodeOptions("data-1", new String[]{"n:2"}, null));
        final var o2 = r.create(NodeObject.class, "n:2", new NodeOptions("data-2", new String[]{"n:3"}, null));
        final var o3 = r.create(NodeObject.class, "n:3", new NodeOptions("data-3", new String[]{"n:4", "n:5"}, new String[]{"n:6"}));
        final var o4 = r.create(NodeObject.class, "n:4", new NodeOptions("data-4", null, null));
        final var o5 = r.create(NodeObject.class, "n:5", new NodeOptions("data-5", null, null));
        final var o6 = r.create(NodeObject.class, "n:6", new NodeOptions("data-6", null, null));

        // Create an instance of SomeFileSystemObject with a specified file path
        final var o7 = r.create(SomeFileSystemObject::new, "fs:1", "/tmp");

        // Get the list of files from the file system object and print them to the console
        o7.get(SomeFileSystemObject::getFilesList).thenAccept(System.out::println);

        // Request a HELLO message from NodeObject 'o1' and print the response
        System.out.println(o1.request(NodeObject.MSG_HELLO).get());

        // Send a broadcast STOP force message to NodeObject 'o5'
        o5.request(Stop.force()).get();

        // Send two HELLO messages to NodeObject 'o6' without expecting a reply
        o6.requestAndForget(NodeObject.MSG_HELLO);
        o6.requestAndForget(NodeObject.MSG_HELLO);

        // Get the counter from NodeObject 'o6' and print the response
        o6.get(NodeObject::getCounter).thenAccept(response -> {
            System.out.println(response);
        });

        // Print the string representation of NodeObjects 'o1' and 'o6'
        System.out.println(o1);
        System.out.println(o6);

        // Perform a load test on NodeObject 'o6' with multiple threads sending HELLO messages
        load(o6);

        // Get the counter from NodeObject 'o6' after the load test and print the response
        System.out.println("total messages: " + o6.get(NodeObject::getCounter).get());
    }

    /**
     * Perform a stress test on an ActiveObject by sending a large number of HELLO messages concurrently
     */
    private static void load(final ActiveObject<?> object) throws InterruptedException, ExecutionException, TimeoutException {
        // Create a fixed thread pool with the specified number of threads
        final var executor = Executors.newFixedThreadPool(THREADS);

        // Initialize two CountDownLatch instances for synchronization purposes
        final var startSignal = new CountDownLatch(THREADS + 1);
        final var doneSignal = new CountDownLatch(1);

        // Submit tasks to the thread pool that will send HELLO messages to the target object
        for (var threadIndex = 0; threadIndex < THREADS; threadIndex++) {
            executor.submit(() -> {
                startSignal.countDown();
                try {
                    // Wait for all threads to be ready before starting the test
                    startSignal.await();
                } catch (final InterruptedException e) {
                    return;
                }

                // Send HELLO messages to the target object in a loop
                for (var msgIndex = 0; msgIndex < MESSAGES; msgIndex++) {
                    object.request(NodeObject.MSG_HELLO);
                }
                // Notify that this thread has completed sending HELLO messages
                doneSignal.countDown();
            });
        }

        // Notify all threads to start the test by counting down the latch
        startSignal.countDown();

        // Wait for all threads to be ready before starting the timer
        startSignal.await();

        // Record the start time of the test
        final var now = System.nanoTime();

        // Wait for all threads to complete sending HELLO messages
        doneSignal.await();

        // Shutdown the executor service after the test is completed
        executor.shutdown();

        // Wait for all pending tasks in the target object's queue to be processed
        ((ActiveObjectContext) object).awaitEmptyQueue(5, TimeUnit.MINUTES);

        // Print the duration of the test in milliseconds
        System.out.println("finished in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - now) + " msec");
    }
}
