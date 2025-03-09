package com.github.devmix.process.acto.core.context;

/**
 * @author Sergey Grachev
 */
public final class InternalDestroy {

    private static final InternalDestroy INSTANCE = new InternalDestroy();

    private InternalDestroy() {
    }

    public static InternalDestroy instance() {
        return INSTANCE;
    }
}
