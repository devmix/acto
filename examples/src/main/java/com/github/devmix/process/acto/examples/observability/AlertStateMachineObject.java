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

package com.github.devmix.process.acto.examples.observability;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.messages.Idle;
import com.github.devmix.process.acto.listeners.OnIdleListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;

/**
 * An alert state machine with a "for" duration: a triggered alert becomes FIRING only if
 * it stays unresolved for the idle timeout — the heartbeat delivers the "pending for 5m"
 * semantics without any timer.
 *
 * @author Sergey Grachev
 */
public class AlertStateMachineObject implements OnMessageListener<AlertStateMachineObject>, OnIdleListener<AlertStateMachineObject> {

    public record Trigger() {
    }

    public record Resolve() {
    }

    public record State() {
    }

    private String state = "OK";

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<AlertStateMachineObject> context) {
        if (message instanceof Trigger) {
            state = "PENDING";
            return state;
        }
        if (message instanceof Resolve) {
            state = "OK";
            return state;
        }
        if (message instanceof State) {
            return state;
        }
        return null;
    }

    @Override
    public boolean onObjectActivate(final Idle.Activate message, final ActiveObjectContext<AlertStateMachineObject> context) {
        return true;
    }

    @Override
    public boolean onObjectDeactivate(final Idle.Deactivate message, final ActiveObjectContext<AlertStateMachineObject> context) {
        if ("PENDING".equals(state)) {
            state = "FIRING";
        }
        return true;
    }
}
