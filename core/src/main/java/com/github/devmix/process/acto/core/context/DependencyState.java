package com.github.devmix.process.acto.core.context;

import com.github.devmix.process.acto.ActiveObject;
import com.github.devmix.process.acto.ActiveObjectDependency;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents the state of a dependency in an active object system.
 *
 * <p>This class holds information about the type, optionality, resolution status,
 * and related error message of a dependency.</p>
 *
 * @author Sergey Grachev
 */
@RequiredArgsConstructor
final class DependencyState implements ActiveObjectDependency {

    /**
     * The type of this dependency.
     */
    @Getter
    private final Type type;

    /**
     * Whether this dependency is optional or not.
     */
    @Getter
    private final boolean optional;

    /**
     * The last error message related to the resolution of this dependency, if any.
     *
     * <p>This field can be set when a dependency fails to resolve.</p>
     */
    @Getter
    @Setter
    private @Nullable String lastError;

    /**
     * Indicates whether this dependency has been resolved.
     */
    @Getter
    private boolean resolved;

    /**
     * The active object associated with this dependency, if it has been resolved.
     */
    @Getter
    private @Nullable ActiveObject<?> object;

    /**
     * Ensures that the associated active object is not null and returns it.
     *
     * <p>This method should be called only when it's guaranteed that the dependency
     * has been successfully resolved.</p>
     *
     * @return The non-null active object associated with this dependency.
     * @throws NullPointerException if the active object is null.
     */
    @Override
    public ActiveObject<?> ensureObject() {
        return Objects.requireNonNull(object);
    }

    /**
     * Executes the given action if this dependency has been resolved.
     *
     * <p>The action will receive the associated active object as its argument.</p>
     *
     * @param action The action to execute if this dependency is resolved.
     */
    @Override
    public void ifResolved(final Consumer<ActiveObject<?>> action) {
        if (resolved) {
            action.accept(object);
        }
    }

    /**
     * Sets the active object associated with this dependency and updates its resolution status.
     *
     * <p>This method sets the {@code resolved} field to true if the given object is not null,
     * otherwise it sets it to false.</p>
     *
     * @param object The new active object to associate with this dependency, or null to reset it.
     */
    void setObject(@Nullable final ActiveObject<?> object) {
        this.object = object;
        this.resolved = object != null;
    }
}
