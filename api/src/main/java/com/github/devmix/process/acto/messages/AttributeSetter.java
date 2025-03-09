package com.github.devmix.process.acto.messages;

/**
 * Functional interface representing an operation that sets a value to a target object.
 *
 * @param <T> the type of the target object
 * @param <V> the type of the value to be set
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface AttributeSetter<T, V> {

    /**
     * Sets the specified value to the given target object.
     *
     * @param target the target object to which the value is to be set
     * @param value  the value to set on the target object
     * @throws Exception if an error occurs during setting of the value
     */
    void set(T target, V value) throws Exception;
}
