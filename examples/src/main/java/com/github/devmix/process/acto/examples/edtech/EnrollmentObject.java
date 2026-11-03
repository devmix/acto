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

import java.util.HashSet;
import java.util.Set;

/**
 * Course enrollment with a seat cap: the counter and the student set are updated in one
 * sequential step, so the cap holds under any enrollment storm.
 *
 * @author Sergey Grachev
 */
public class EnrollmentObject implements OnMessageListener<EnrollmentObject> {

    public record Enroll(String student) {
    }

    public record Enrolled() {
    }

    private final int seats;
    private final Set<String> students = new HashSet<>();

    public EnrollmentObject(final Object id, final Integer seats) {
        this.seats = seats;
    }

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<EnrollmentObject> context) {
        if (message instanceof Enroll enroll) {
            if (students.contains(enroll.student())) {
                return Boolean.TRUE;
            }
            if (students.size() < seats) {
                students.add(enroll.student());
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if (message instanceof Enrolled) {
            return students.size();
        }
        return null;
    }
}
