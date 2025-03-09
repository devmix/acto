package com.github.devmix.process.acto.fixtures;

import com.github.devmix.process.acto.core.context.DefaultActiveObjectContext;
import lombok.Getter;

import java.util.List;

/**
 * @author Sergey Grachev
 */
@Getter
public final class EchoOptions {

    private final List<String> dependsOn;
    private final List<String> dependsOnOptional;
    private final boolean allowIdle;
    private final long idleTimeout;

    public EchoOptions(final boolean allowIdle, final long idleTimeout, final List<String> dependsOn, final List<String> dependsOnOptional) {
        this.idleTimeout = idleTimeout;
        this.dependsOn = dependsOn;
        this.dependsOnOptional = dependsOnOptional;
        this.allowIdle = allowIdle;
    }

    public EchoOptions(final List<String> dependsOn, final List<String> dependsOnOptional) {
        this(false, DefaultActiveObjectContext.NO_DEACTIVATION_TIMEOUT, dependsOn, dependsOnOptional);
    }

    public EchoOptions(final boolean allowIdle, final long idleTimeout) {
        this(allowIdle, idleTimeout, null, null);
    }
}
