package com.github.devmix.process.acto;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Represents a dependency between active objects.
 *
 * @author Sergey Grachev
 */
public interface ActiveObjectDependency {
    /**
     * Ensures that the object is resolved and returns it. Otherwise, this method will throw a NullPointerException
     * if the object is not yet resolved.
     *
     * @return The resolved {@link ActiveObject}.
     * @throws NullPointerException if the object has not been resolved yet
     */
    ActiveObject<?> ensureObject();

    /**
     * Returns the object if it has already been resolved, or null otherwise.
     *
     * @return The resolved {@link ActiveObject}, or null if not yet resolved.
     */
    @Nullable
    ActiveObject<?> getObject();

    /**
     * Returns the last error message that occurred during the resolution process,
     * or an empty string if no errors have occurred.
     *
     * @return The last error message, or an empty string if no errors have occurred.
     */
    String getLastError();

    /**
     * Returns the type of dependency relationship.
     *
     * @return The {@link Type} of this dependency.
     */
    Type getType();

    /**
     * Executes the provided action if and when the object is resolved. If the
     * object is already resolved, the action will be executed immediately.
     * Otherwise, it will be queued for execution once resolution occurs.
     *
     * @param action The {@link Consumer} to execute with the resolved {@link ActiveObject}.
     */
    void ifResolved(Consumer<ActiveObject<?>> action);

    /**
     * Indicates whether this dependency is optional. An optional dependency
     * can be unresolved without causing an error.
     *
     * @return True if this dependency is optional, false otherwise.
     */
    boolean isOptional();

    /**
     * Checks whether the object has been resolved and is available for use.
     *
     * @return True if the object is resolved, false otherwise.
     */
    boolean isResolved();

    /**
     * Enumerates the types of relationships that can exist between active objects.
     */
    enum Type {
        /**
         * Indicates a dependency where one object depends on another.
         */
        DEPENDS_ON,

        /**
         * Indicates a dependency where one object is required for another.
         */
        REQUIRED_FOR,
    }
}
