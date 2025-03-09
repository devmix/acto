package com.github.devmix.process.acto.fixtures.test;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Sergey Grachev
 */
@RequiredArgsConstructor
@Getter
public final class NodeOptions {

    private final String state;
    private final String[] dependsOn;
    private final String[] dependsOnOptional;
}
