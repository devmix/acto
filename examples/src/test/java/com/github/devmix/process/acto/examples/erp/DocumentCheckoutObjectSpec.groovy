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

package com.github.devmix.process.acto.examples.erp

import com.github.devmix.process.acto.core.config.DispatcherConfig
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class DocumentCheckoutObjectSpec extends Specification {

    def 'only one editor holds the document, and a forgotten checkout is auto-released on idle'() {
        given:
        def config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def doc = registry.create({ id, options -> new DocumentCheckoutObject() }, 'doc-1', null)

        when: 'two users race for the document'
        def results = [doc.request(new DocumentCheckoutObject.Checkout('alice')),
                       doc.request(new DocumentCheckoutObject.Checkout('bob'))]
                *.get(5, TimeUnit.SECONDS)

        then: 'exactly one wins'
        results.count { it == Boolean.TRUE } == 1
        def holder = doc.<String>request(new DocumentCheckoutObject.Holder()).get(5, TimeUnit.SECONDS)
        holder != null

        when: 'the holder checks in'
        doc.request(new DocumentCheckoutObject.Checkin(holder)).get(5, TimeUnit.SECONDS)

        then:
        doc.<String>request(new DocumentCheckoutObject.Holder()).get(5, TimeUnit.SECONDS) == null

        when: 'a checkout is forgotten and the object sits idle'
        doc.request(new DocumentCheckoutObject.Checkout('carol')).get(5, TimeUnit.SECONDS)
        Thread.sleep(500)

        then: 'the heartbeat released the lock'
        doc.<Integer>request(new DocumentCheckoutObject.AutoReleases()).get(5, TimeUnit.SECONDS) >= 1
        doc.<String>request(new DocumentCheckoutObject.Holder()).get(5, TimeUnit.SECONDS) == null

        cleanup:
        registry.shutdown(true)
    }
}
