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

package com.github.devmix.process.acto.examples.medtech;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

/**
 * A lab order state machine: ordered transitions from order to result, with illegal
 * transitions rejected in the mailbox.
 *
 * @author Sergey Grachev
 */
public class LabOrderObject implements OnMessageListener<LabOrderObject> {

    public enum State {ORDERED, COLLECTED, IN_LAB, RESULTED}

    public record Collect() {
    }

    public record StartAnalysis() {
    }

    public record PublishResult(String value) {
    }

    public record StateQuery() {
    }

    private State state = State.ORDERED;
    private String result;

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<LabOrderObject> context) {
        if (message instanceof Collect) {
            return transition(State.ORDERED, State.COLLECTED);
        }
        if (message instanceof StartAnalysis) {
            return transition(State.COLLECTED, State.IN_LAB);
        }
        if (message instanceof PublishResult publish) {
            if (state != State.IN_LAB) {
                return "ILLEGAL:" + state;
            }
            state = State.RESULTED;
            result = publish.value();
            return state;
        }
        if (message instanceof StateQuery) {
            return result == null ? state : state + ":" + result;
        }
        return null;
    }

    private Object transition(final State expected, final State next) {
        if (state == expected) {
            state = next;
            return state;
        }
        return "ILLEGAL:" + state;
    }
}
