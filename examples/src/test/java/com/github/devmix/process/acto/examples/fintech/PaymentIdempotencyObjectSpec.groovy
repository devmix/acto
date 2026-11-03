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

package com.github.devmix.process.acto.examples.fintech

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class PaymentIdempotencyObjectSpec extends Specification {

    def 'a retry storm with the same idempotency key charges exactly once'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def payments = registry.create({ id, options -> new PaymentIdempotencyObject() }, 'payments', null)

        when: 'the same payment is retried 50 times concurrently'
        def results = (1..50).collect { payments.request(new PaymentIdempotencyObject.Pay('key-1', 1000L)) }
                *.get(5, TimeUnit.SECONDS)

        then: 'every retry gets the cached result and the ledger charges once'
        results.toSet() == ['CHARGED:1000'].toSet()
        payments.<Long>request(new PaymentIdempotencyObject.Charged()).get(5, TimeUnit.SECONDS) == 1000L

        and: 'a different key is a new charge'
        payments.<String>request(new PaymentIdempotencyObject.Pay('key-2', 500L)).get(5, TimeUnit.SECONDS) == 'CHARGED:500'
        payments.<Long>request(new PaymentIdempotencyObject.Charged()).get(5, TimeUnit.SECONDS) == 1500L

        cleanup:
        registry.shutdown(true)
    }
}
