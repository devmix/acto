package com.github.devmix.process.acto.core.context;

/**
 * @author Sergey Grachev
 */
final class InternalHeartbeat {

    public static final InternalHeartbeat INSTANCE = new InternalHeartbeat();

    private InternalHeartbeat() {
    }

    public static InternalHeartbeat standard() {
        return INSTANCE;
    }
}
