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

package com.github.devmix.process.acto.examples.ledger

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import com.github.devmix.process.acto.exceptions.DispatchingException
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class LedgerExampleSpec extends Specification {

    def 'graceful destroy journals every transaction accepted before the destroy request'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def ledger = registry.create({ id, options -> new TransactionLedger() }, 'ledger', null)

        when: '10 producers append 100 transactions each'
        def futures = Collections.synchronizedList(new ArrayList<CompletableFuture<Object>>())
        def producers = (1..10).collect { t ->
            Thread.start { 100.times { i -> futures << ledger.request(new TransactionLedger.Append("tx-$t-$i")) } }
        }
        producers.each { it.join() }

        and: 'the ledger is gracefully destroyed'
        def destroyed = registry.destroy('ledger', false)

        then: 'every accepted transaction is journaled, none is lost'
        destroyed
        futures.every { it.get(5, TimeUnit.SECONDS) != null }
        ledger.instance.journalView().size() == 1_000

        and: 'journal sequence numbers are unique'
        futures.collect { it.get(5, TimeUnit.SECONDS) }.toSet().size() == 1_000

        cleanup:
        registry.shutdown(true)
    }

    def 'appends after destroy are rejected, not silently dropped'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def ledger = registry.create({ id, options -> new TransactionLedger() }, 'ledger', null)
        registry.destroy('ledger', false)

        when:
        ledger.request(new TransactionLedger.Append('too-late')).get(5, TimeUnit.SECONDS)

        then:
        def e = thrown(ExecutionException)
        e.cause instanceof DispatchingException

        cleanup:
        registry.shutdown(true)
    }
}
