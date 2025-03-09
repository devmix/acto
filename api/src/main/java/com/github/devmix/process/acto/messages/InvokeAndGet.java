package com.github.devmix.process.acto.messages;

/**
 * Functional interface representing an operation that takes a single argument and returns a result,
 * allowing for checked exceptions to be thrown during its execution.
 *
 * @param <T> the type of the input to the run method
 * @param <R> the type of the result of the run method
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface InvokeAndGet<T, R> {

    /**
     * Executes the operation using the specified argument.
     *
     * @param object the input argument to the operation
     * @return the result of the operation
     * @throws Exception if an error occurs during the execution of the operation
     */
    R run(T object) throws Exception;
}
