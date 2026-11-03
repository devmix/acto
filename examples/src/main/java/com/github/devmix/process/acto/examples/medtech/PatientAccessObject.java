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
import com.github.devmix.process.acto.messages.Idle;
import com.github.devmix.process.acto.listeners.OnIdleListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A patient record access session: access is granted per user, every grant and the
 * automatic idle close are written to an audit trail — the HIPAA-style "close the record
 * when the clinician walks away" rule is just the idle timeout.
 *
 * @author Sergey Grachev
 */
public class PatientAccessObject implements OnMessageListener<PatientAccessObject>, OnIdleListener<PatientAccessObject> {

    public record Open(String user) {
    }

    public record Access() {
    }

    public record Audit() {
    }

    private String user;
    private final List<String> audit = new ArrayList<>();

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<PatientAccessObject> context) {
        if (message instanceof Open open) {
            user = open.user();
            audit.add("OPEN:" + open.user());
            return Boolean.TRUE;
        }
        if (message instanceof Access) {
            return user;
        }
        if (message instanceof Audit) {
            return List.copyOf(audit);
        }
        return null;
    }

    @Override
    public boolean onObjectActivate(final Idle.Activate message, final ActiveObjectContext<PatientAccessObject> context) {
        return true;
    }

    @Override
    public boolean onObjectDeactivate(final Idle.Deactivate message, final ActiveObjectContext<PatientAccessObject> context) {
        if (user != null) {
            audit.add("AUTO_CLOSE:" + user);
            user = null;
        }
        return true;
    }
}
