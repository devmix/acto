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

package com.github.devmix.process.acto.examples.bi;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.messages.Idle;
import com.github.devmix.process.acto.listeners.OnIdleListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User session windowization: events extend the active session of a user, and the idle
 * timeout closes all sessions at once — the classic "session = 30 minutes of inactivity"
 * rule implemented with zero timers.
 *
 * @author Sergey Grachev
 */
public class SessionWindowizerObject implements OnMessageListener<SessionWindowizerObject>, OnIdleListener<SessionWindowizerObject> {

    public record Event(String user) {
    }

    public record Closed() {
    }

    public record Active() {
    }

    private final Map<String, Integer> active = new HashMap<>();
    private final List<String> closed = new ArrayList<>();

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<SessionWindowizerObject> context) {
        if (message instanceof Event event) {
            active.merge(event.user(), 1, Integer::sum);
            return active.size();
        }
        if (message instanceof Closed) {
            return List.copyOf(closed);
        }
        if (message instanceof Active) {
            return active.size();
        }
        return null;
    }

    @Override
    public boolean onObjectActivate(final Idle.Activate message, final ActiveObjectContext<SessionWindowizerObject> context) {
        return true;
    }

    @Override
    public boolean onObjectDeactivate(final Idle.Deactivate message, final ActiveObjectContext<SessionWindowizerObject> context) {
        active.forEach((user, count) -> closed.add(user + ":" + count));
        active.clear();
        return true;
    }
}
