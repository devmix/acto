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

package com.github.devmix.process.acto.examples.edtech;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.ArrayList;
import java.util.List;

/**
 * An append-only learning-progress journal: events from many devices are journaled in the
 * order the mailbox accepted them, and the whole progress state is replayable from the log.
 *
 * @author Sergey Grachev
 */
public class ProgressJournalObject implements OnMessageListener<ProgressJournalObject> {

    public record Record(String event) {
    }

    public record Replay() {
    }

    private final List<String> journal = new ArrayList<>();

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<ProgressJournalObject> context) {
        if (message instanceof Record record) {
            journal.add(record.event());
            return journal.size();
        }
        if (message instanceof Replay) {
            return List.copyOf(journal);
        }
        return null;
    }
}
