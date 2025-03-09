package com.github.devmix.process.acto.core.context;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an entry in a message queue that includes a message and a CompletableFuture to handle the response.
 *
 * @param <T> The type of the result expected for the future.
 * @author Sergey Grachev
 */
record QueueEntry<T>(
        /**
         * The message to be processed by the queue.
         */
        Object message,
        /**
         * The CompletableFuture that will be completed with the result of processing the message.
         * This can be null if no response is expected.
         */
        CompletableFuture<T> future
) {
    /**
     * Creates a new QueueEntry with a given message and no expected response.
     *
     * @param message The message to be processed.
     * @return A new instance of QueueEntry with the provided message and a null future.
     */
    public static QueueEntry<?> noResponse(final Object message) {
        return new QueueEntry<>(message, null);
    }

    /**
     * Completes the future associated with this QueueEntry successfully with the given result.
     * If the future is null or an exception occurs during completion, nothing happens.
     *
     * @param result The result to complete the future with. Can be null if appropriate for the CompletableFuture type.
     */
    public void success(@Nullable final Object result) {
        if (future != null) {
            try {
                //noinspection unchecked
                future.complete((T) result);
            } catch (final Exception e) {
                future.completeExceptionally(e);
            }
        }
    }

    /**
     * Completes the future associated with this QueueEntry exceptionally with the given exception.
     * If the future is null, nothing happens.
     *
     * @param exception The exception to complete the future with.
     */
    public void fail(final Exception exception) {
        if (future != null) {
            future.completeExceptionally(exception);
        }
    }
}
