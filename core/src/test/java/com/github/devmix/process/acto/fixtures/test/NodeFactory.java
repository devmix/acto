package com.github.devmix.process.acto.fixtures.test;

import com.github.devmix.process.acto.ActiveObjectFactory;

import javax.annotation.Nullable;

/**
 * @author Sergey Grachev
 */
public final class NodeFactory implements ActiveObjectFactory<NodeObject, String, NodeOptions> {

    @Override
    public NodeObject create(final String id, @Nullable final NodeOptions options) {
        return new NodeObject(id, options);
    }
}
