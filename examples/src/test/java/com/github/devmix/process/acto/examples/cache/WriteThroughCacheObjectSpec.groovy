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

package com.github.devmix.process.acto.examples.cache

import com.github.devmix.process.acto.core.config.DispatcherConfig
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification
import spock.lang.Timeout

import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class WriteThroughCacheObjectSpec extends Specification {

    @Timeout(30)
    def 'concurrent writes are visible, idle deactivation drops the cache but not the store'() {
        given: 'a cache with an aggressive heartbeat'
        def config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def cache = registry.create({ id, options -> new WriteThroughCacheObject() }, 'cache', null)

        when: '160 puts are issued concurrently'
        def futures = (0..<160).collect { i -> cache.request(new WriteThroughCacheObject.Put("key-$i", "value-$i")) }

        then: 'every write-through value is readable'
        futures*.get(30, TimeUnit.SECONDS).every { it == Boolean.TRUE }
        (0..<160).every { i ->
            cache.<String>request(new WriteThroughCacheObject.Get("key-$i")).get(5, TimeUnit.SECONDS) == "value-$i"
        }

        and: 'the backing store kept every write'
        cache.<List<String>>request(new WriteThroughCacheObject.Persisted()).get(5, TimeUnit.SECONDS).size() == 160

        when: 'the object sits idle past the idle timeout'
        Thread.sleep(500)

        then: 'the heartbeat deactivated it and the in-memory cache was dropped'
        cache.<Integer>request(new WriteThroughCacheObject.Deactivations()).get(5, TimeUnit.SECONDS) >= 1
        cache.<String>request(new WriteThroughCacheObject.Get("key-0")).get(5, TimeUnit.SECONDS) == null

        and: 'the persisted store still holds the data'
        cache.<List<String>>request(new WriteThroughCacheObject.Persisted()).get(5, TimeUnit.SECONDS).size() == 160

        cleanup:
        registry.shutdown(true)
    }
}
