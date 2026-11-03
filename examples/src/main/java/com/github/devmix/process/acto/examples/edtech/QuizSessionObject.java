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
import com.github.devmix.process.acto.messages.Idle;
import com.github.devmix.process.acto.listeners.OnIdleListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A quiz attempt session: answers are collected while the student is active, and the
 * heartbeat auto-submits the attempt when the student goes silent for the idle timeout —
 * the exam timer is the dispatcher heartbeat, not a scheduled thread.
 *
 * @author Sergey Grachev
 */
public class QuizSessionObject implements OnMessageListener<QuizSessionObject>, OnIdleListener<QuizSessionObject> {

    public record Answer(int question, String answer) {
    }

    public record State() {
    }

    public record Answers() {
    }

    private final List<String> answers = new ArrayList<>();
    private String state = "IN_PROGRESS";

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<QuizSessionObject> context) {
        if (message instanceof Answer answer) {
            if ("IN_PROGRESS".equals(state)) {
                answers.add(answer.question() + ":" + answer.answer());
            }
            return state;
        }
        if (message instanceof State) {
            return state;
        }
        if (message instanceof Answers) {
            return List.copyOf(answers);
        }
        return null;
    }

    @Override
    public boolean onObjectActivate(final Idle.Activate message, final ActiveObjectContext<QuizSessionObject> context) {
        return true;
    }

    @Override
    public boolean onObjectDeactivate(final Idle.Deactivate message, final ActiveObjectContext<QuizSessionObject> context) {
        if ("IN_PROGRESS".equals(state)) {
            state = "SUBMITTED";
        }
        return true;
    }
}
