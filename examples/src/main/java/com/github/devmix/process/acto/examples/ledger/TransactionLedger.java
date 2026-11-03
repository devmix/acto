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

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An append-only transaction journal hidden behind an active object.
 *
 * <p>The journal list is touched only from the object dispatch thread, so no locking is
 * required anywhere in this class. Producers may append from any number of threads.</p>
 *
 * @author Sergey Grachev
 */
public class TransactionLedger implements OnMessageListener<TransactionLedger> {

    /**
     * Append a transaction to the journal. The future completes with the journal size
     * after the append, i.e. the sequence number of the accepted transaction.
     */
    public record Append(String transaction) {
    }

    /**
     * Read a snapshot of the journal.
     */
    public record Read() {
    }

    private final List<String> journal = new ArrayList<>();

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<TransactionLedger> context) {
        if (message instanceof Append append) {
            journal.add(append.transaction());
            return journal.size();
        }
        if (message instanceof Read) {
            return List.copyOf(journal);
        }
        return null;
    }

    /**
     * Read-only view of the journal. Only safe to call after the object has been destroyed
     * (the dispatch thread is guaranteed to be finished), which is exactly what the example does.
     */
    public List<String> journalView() {
        return Collections.unmodifiableList(journal);
    }
}
