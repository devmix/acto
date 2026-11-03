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

package com.github.devmix.process.acto.examples.misc;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A saga coordinator: step results arrive as messages, and a failure triggers compensation
 * of the completed steps in reverse order — the saga state machine is sequential code.
 *
 * @author Sergey Grachev
 */
public class SagaCoordinatorObject implements OnMessageListener<SagaCoordinatorObject> {

    public record StepOk(int step) {
    }

    public record StepFail(int step) {
    }

    public record State() {
    }

    public record Compensations() {
    }

    private final List<Integer> completed = new ArrayList<>();
    private final List<String> compensations = new ArrayList<>();
    private String state = "RUNNING";

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<SagaCoordinatorObject> context) {
        if (message instanceof StepOk ok) {
            if ("RUNNING".equals(state)) {
                completed.add(ok.step());
            }
            return state;
        }
        if (message instanceof StepFail fail) {
            if ("RUNNING".equals(state)) {
                state = "COMPENSATING";
                for (var i = completed.size() - 1; i >= 0; i--) {
                    compensations.add("compensate:" + completed.get(i));
                }
                state = "COMPENSATED";
            }
            return state;
        }
        if (message instanceof State) {
            return state;
        }
        if (message instanceof Compensations) {
            return List.copyOf(compensations);
        }
        return null;
    }
}
