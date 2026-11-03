/*
 * devMix · Process · Active Objects [ActO]
 * Copyright (C) 2025, Sergey Grachev <sergey.grachev@yahoo.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.devmix.process.acto.core.config;

import com.github.devmix.process.acto.DispatcherMetrics;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Immutable configuration of {@code DefaultActiveObjectsDispatcher} and the active object contexts
 * created by it.
 *
 * <p>Use {@link #standard()} for the default values or {@link #builder()} to override them.
 * {@code builder().build()} validates the parameters: every timeout must be positive
 * ({@code messageTimeout} may be {@code ZERO} to disable the deadline), and a warning is logged
 * when {@code idleTimeout} is shorter than {@code heartbeatInterval} (deactivation then fires
 * late and unevenly). Invalid values fail with {@link IllegalArgumentException}.</p>
 *
 * @author Sergey Grachev
 */
@Builder(builderMethodName = "rawBuilder")
@Getter
public final class DispatcherConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DispatcherConfig.class);

    /**
     * How often the heartbeat is sent to the active objects to detect idle ones.
     */
    @Builder.Default
    private final Duration heartbeatInterval = Duration.ofMinutes(5);

    /**
     * How long an active object must be inactive before it is deactivated.
     */
    @Builder.Default
    private final Duration idleTimeout = Duration.ofMinutes(5);

    /**
     * How long the dispatch task waits for new messages before terminating.
     */
    @Builder.Default
    private final Duration waitMessagesTimeout = Duration.ofMillis(500);

    /**
     * Default deadline for user messages sent through {@code request()}: the returned future
     * completes with {@code TimeoutException} if the message is not processed within this
     * time. A timed-out or cancelled message is skipped when the dispatcher reaches it, so a
     * stuck object does not execute work nobody waits for anymore. {@code ZERO} disables
     * the deadline.
     */
    @Builder.Default
    private final Duration messageTimeout = Duration.ZERO;

    /**
     * How long {@code destroy()} waits for the object to be destroyed.
     */
    @Builder.Default
    private final Duration destroyTimeout = Duration.ofSeconds(15);

    /**
     * How long the start of a dependency is awaited.
     */
    @Builder.Default
    private final Duration dependencyStartTimeout = Duration.ofSeconds(15);

    /**
     * How long the stop of a dependency is awaited.
     */
    @Builder.Default
    private final Duration dependencyStopTimeout = Duration.ofSeconds(15);

    /**
     * When {@code true}, lifecycle events are logged on the INFO level; otherwise on the DEBUG level.
     */
    @Builder.Default
    private final boolean debugLifecycleLogging = false;

    /**
     * The maximum number of user messages waiting in the queue of each active object.
     * {@code 0} or a negative value means unlimited.
     *
     * <p>The limit is soft: concurrent producers may overshoot it by a few messages because
     * the size check and the insertion are not atomic. It bounds memory growth, not the
     * exact queue size.</p>
     */
    @Builder.Default
    private final int queueCapacity = 0;

    /**
     * The behavior when a user message arrives while the object queue is full.
     */
    @Builder.Default
    private final QueueOverflowPolicy queueOverflowPolicy = QueueOverflowPolicy.FAIL;

    /**
     * Observability hooks. The default {@link DispatcherMetrics#NOOP} adds no behavior.
     */
    @Builder.Default
    private final DispatcherMetrics metrics = DispatcherMetrics.NOOP;

    /**
     * Returns a builder that validates the configuration on {@code build()}.
     *
     * @return a validating builder
     */
    public static DispatcherConfigBuilder builder() {
        return new ValidatingBuilder();
    }

    /**
     * Returns a validating builder pre-populated with this configuration's values.
     *
     * @return a validating builder
     */
    public DispatcherConfigBuilder toBuilder() {
        return builder()
                .heartbeatInterval(heartbeatInterval)
                .idleTimeout(idleTimeout)
                .waitMessagesTimeout(waitMessagesTimeout)
                .messageTimeout(messageTimeout)
                .destroyTimeout(destroyTimeout)
                .dependencyStartTimeout(dependencyStartTimeout)
                .dependencyStopTimeout(dependencyStopTimeout)
                .debugLifecycleLogging(debugLifecycleLogging)
                .queueCapacity(queueCapacity)
                .queueOverflowPolicy(queueOverflowPolicy)
                .metrics(metrics);
    }

    private static final class ValidatingBuilder extends DispatcherConfigBuilder {

        @Override
        public DispatcherConfig build() {
            return validate(super.build());
        }
    }

    private static DispatcherConfig validate(final DispatcherConfig config) {
        requirePositive("heartbeatInterval", config.heartbeatInterval);
        requirePositive("idleTimeout", config.idleTimeout);
        requirePositive("waitMessagesTimeout", config.waitMessagesTimeout);
        requirePositive("destroyTimeout", config.destroyTimeout);
        requirePositive("dependencyStartTimeout", config.dependencyStartTimeout);
        requirePositive("dependencyStopTimeout", config.dependencyStopTimeout);
        if (config.messageTimeout == null || config.messageTimeout.isNegative()) {
            throw new IllegalArgumentException("messageTimeout must not be negative: " + config.messageTimeout);
        }
        if (config.idleTimeout.compareTo(config.heartbeatInterval) < 0) {
            LOG.warn("idleTimeout [{}] is shorter than heartbeatInterval [{}]: "
                            + "idle detection only runs on heartbeats, so deactivation will fire late and unevenly",
                    config.idleTimeout, config.heartbeatInterval);
        }
        return config;
    }

    private static void requirePositive(final String name, final Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }

    /**
     * Returns a configuration with all default values.
     *
     * @return the standard configuration
     */
    public static DispatcherConfig standard() {
        return builder().build();
    }
}
