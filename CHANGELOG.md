# Changelog

All notable changes to this project are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0] - 2026-11-03

### Added

- Backpressure: `DispatcherConfig.queueCapacity` bounds the queued user messages per object,
  `QueueOverflowPolicy` chooses between `FAIL` (`QueueOverflowException`) and `BLOCK`
  (producer waits for a free slot); `ActiveObjectContext.getRejectedMessages()` counts rejections
- Message deadlines: `DispatcherConfig.messageTimeout` completes `request()` futures with
  `TimeoutException` when the object does not process the message in time; timed-out or cancelled
  messages are skipped instead of being executed for a caller that has given up
- `examples` module: 33 runnable, tested use cases grouped by domain, each rated for how well it
  fits the active object pattern
- Observability: `DispatcherMetrics` hooks (processed/rejected messages, queue depth, destroyed
  objects) with a zero-overhead no-op default and an optional `MicrometerDispatcherMetrics`
  backend (`micrometer-core` is an optional dependency)
- `benchmarks` module (JMH, profile `-Pbenchmarks`, not part of the default build) with a
  committed baseline under `docs/benchmarks/`
- Dispatch threads are named: the dispatch loop runs on a virtual thread named
  `acto-dispatch-<objectId>`, so logs and thread dumps identify the owning object
- `DispatcherConfig.builder().build()` validates its parameters: timeouts must be positive
  (`messageTimeout` may be `ZERO` to disable the deadline), with a warning when
  `idleTimeout` is shorter than `heartbeatInterval`
- JPMS module descriptors (`devmix.process.acto.api`, `devmix.process.acto.core`) and a japicmp
  binary-compatibility gate in CI
- `CONTRIBUTING.md`
- `DispatcherConfig` as the single configuration entry point: `heartbeatInterval`, `idleTimeout`,
  `waitMessagesTimeout`, `destroyTimeout`, `dependencyStartTimeout`, `dependencyStopTimeout`,
  `debugLifecycleLogging`
- `DefaultActiveObjectsDispatcher(DispatcherConfig)` constructor
- `OnIdleListener` with `onObjectActivate` / `onObjectDeactivate`, driven by the dispatcher heartbeat
- `ActiveObjectStatus` exposes the object state to callers
- `Start` / `Stop` carry the dependency chain, so cyclic dependencies are detected instead of hanging
- `debugLifecycleLogging` logs the full lifecycle on the INFO level

### Changed

- CI caches the Temurin JDK archive and provisions it without `setup-java` (the archive is
  extracted locally; `setup-java@v5` removed local-file installation upstream)
- CI runs SpotBugs + findsecbugs, ErrorProne and a JaCoCo line-coverage gate on `core`
- `catch (Exception)` around dependency start/stop narrowed to the concrete exceptions
  (`Error`s are no longer swallowed, the interrupt flag is restored)
- `slf4j-simple` is a test-scoped dependency of `core` and no longer leaks to consumers
- **Breaking** `destroy(id, false)` is now graceful: the object processes every message accepted
  before the destroy request and only then is destroyed. In 0.1.x destroy jumped the queue
  immediately, so accepted work could be lost
- **Breaking** `destroy(id, true)` discards the messages still in the queue; their futures complete
  exceptionally with `DispatchingException`. Messages sent after a destroy request always fail fast
- **Breaking** `destroy()` never throws: a failed or timed-out destruction is reported by `false`,
  and the object stays in the registry
- **Breaking** `ActiveObjectContext.awaitEmptyQueue` no longer declares `ExecutionException`
- `getWaitMessagesTimeout` is documented in milliseconds (it was already returning milliseconds)
- `getIdleTimeout` is documented in nanoseconds
- `awaitEmptyQueue` also waits for the in-flight message to finish

### Deprecated

- `DefaultActiveObjectsDispatcher(long heartbeatInterval)` — use `DispatcherConfig`
- `DefaultActiveObjectsDispatcher.setHeartbeatInterval(long)` — use `DispatcherConfig`
- `DefaultActiveObjectContext.DEBUGGING_INFO` — use `DispatcherConfig.debugLifecycleLogging`

### Migration from 0.1.x

1. Code that relied on destroy being immediate should pass `force = true`:

   ```java
   // 0.1.x: destroy(id, false) jumped the queue; in 0.2.0 it drains it first
   registry.destroy(id, true);
   ```

   Keep `force = false` when accepted work must not be lost — that is the point of the change.

2. Callers that caught `ExecutionException` around `awaitEmptyQueue` can drop it; the method now
   declares only `InterruptedException` and `TimeoutException`.

3. Callers that caught exceptions from `destroy()` should check the `boolean` result instead:

   ```java
   if (!registry.destroy(id, false)) {
       // unknown id, destruction failed or timed out; the object is still in the registry
   }
   ```

4. Replace the deprecated constructors and setters with a single configuration object:

   ```java
   var dispatcher = new DefaultActiveObjectsDispatcher(DispatcherConfig.builder()
           .heartbeatInterval(Duration.ofMinutes(5))
           .idleTimeout(Duration.ofMinutes(5))
           .build());
   ```

5. Replace `DefaultActiveObjectContext.DEBUGGING_INFO = true` with
   `DispatcherConfig.builder().debugLifecycleLogging(true)`, and `setHeartbeatInterval(ms)`
   with `DispatcherConfig.builder().heartbeatInterval(Duration.ofMillis(ms))`.

## [0.1.0] - 2025-03-19

### Added

- Initial public release: `ActiveObjectsDispatcher`, `ActiveObjectContext`, `ActiveObjectFactory`,
  `ActiveObjectLifecycle`, object dependencies (`DEPENDS_ON` / `REQUIRED_FOR`),
  lifecycle listeners (`OnCreate`, `OnStart`, `OnMessage`, `OnStop`, `OnDestroy`),
  `Start` / `Stop` messages and the `Invoke` / `AttributeGetter` / `AttributeSetter` accessors
