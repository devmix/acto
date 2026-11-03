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

package com.github.devmix.process.acto.examples.ledger;

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Use case 1: a financial transaction journal behind an active object.
 *
 * <p>Ten producer threads append transactions as fast as they can, then the ledger is
 * <b>gracefully</b> destroyed ({@code destroy(id, false)}). The graceful destroy guarantees
 * that every transaction accepted before the destroy request is journaled before the
 * object shuts down — nothing accepted is lost.</p>
 *
 * @author Sergey Grachev
 */
public final class LedgerExample {

    private static final int PRODUCERS = 10;
    private static final int TRANSACTIONS_PER_PRODUCER = 100;

    private LedgerExample() {
    }

    public static void main(final String[] args) throws InterruptedException {
        final var registry = new DefaultActiveObjectsDispatcher();
        final var ledger = registry.create((id, options) -> new TransactionLedger(), "ledger", null);

        // producers append concurrently; each future completes with the journal sequence number
        final List<CompletableFuture<Object>> accepted = Collections.synchronizedList(new ArrayList<>());
        final var producers = new ArrayList<Thread>();
        for (var producer = 0; producer < PRODUCERS; producer++) {
            final var id = producer;
            producers.add(Thread.ofVirtual().start(() -> {
                for (var i = 0; i < TRANSACTIONS_PER_PRODUCER; i++) {
                    accepted.add(ledger.request(new TransactionLedger.Append("tx-" + id + "-" + i)));
                }
            }));
        }
        for (final var producer : producers) {
            producer.join();
        }

        // graceful destroy: everything accepted above is journaled before the object dies
        final var destroyed = registry.destroy("ledger", false);

        final var journal = ledger.getInstance().journalView();
        System.out.println("destroyed: " + destroyed);
        System.out.println("accepted:  " + accepted.size());
        System.out.println("journaled: " + journal.size());
        System.out.println("first: " + journal.get(0) + ", last: " + journal.get(journal.size() - 1));

        registry.shutdown(true);
    }
}
