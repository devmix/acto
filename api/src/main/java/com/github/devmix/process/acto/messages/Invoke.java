package com.github.devmix.process.acto.messages;

/**
 * Functional interface representing an operation that accepts a single input argument and performs some action.
 *
 * @param <T> the type of the input to the {@code run} method
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface Invoke<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param object the input argument
     * @throws Exception if an error occurs while performing the operation
     */
    void run(T object) throws Exception;
}
