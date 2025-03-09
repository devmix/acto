package com.github.devmix.process.acto.messages;

/**
 * Represents a function that extracts an attribute from a target object.
 *
 * <p>This functional interface is designed to be used in contexts where an attribute or property
 * of an object needs to be retrieved, potentially throwing an exception if the operation fails.</p>
 *
 * @param <T> The type of the input object (the target from which the attribute will be extracted).
 * @param <R> The type of the result (the attribute being extracted).
 * @author Sergey Grachev
 */
@FunctionalInterface
public interface AttributeGetter<T, R> {

    /**
     * Extracts an attribute from the given target.
     *
     * @param target the object from which to extract the attribute
     * @return the extracted attribute
     * @throws Exception if any error occurs during the extraction process
     */
    R get(T target) throws Exception;
}
