package com.github.devmix.process.acto.core.context;

import com.github.devmix.process.acto.messages.AttributeSetter;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * @author Sergey Grachev
 */
@Getter
class InternalSetAttribute {

    private final AttributeSetter<Object, Object> action;
    private final @Nullable Object value;

    private InternalSetAttribute(final AttributeSetter<Object, Object> action, @Nullable final Object value) {
        this.action = action;
        this.value = value;
    }

    public static Object create(final AttributeSetter<?, ?> action, @Nullable final Object value) {
        //noinspection unchecked
        return new InternalSetAttribute((AttributeSetter<Object, Object>) action, value);
    }
}
