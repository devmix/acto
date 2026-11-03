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

package com.github.devmix.process.acto.examples.erp;

import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.listeners.OnMessageListener;

/**
 * Invoice approval workflow as a sequential state machine: illegal transitions are simply
 * rejected in the mailbox, so concurrent submitters cannot corrupt the state.
 *
 * @author Sergey Grachev
 */
public class InvoiceApprovalObject implements OnMessageListener<InvoiceApprovalObject> {

    public enum State {DRAFT, PENDING_APPROVAL, APPROVED, POSTED, REJECTED}

    public record Submit() {
    }

    public record Approve(String user) {
    }

    public record Reject(String user) {
    }

    public record Post() {
    }

    public record StateQuery() {
    }

    private State state = State.DRAFT;

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<InvoiceApprovalObject> context) {
        if (message instanceof Submit) {
            return transition(State.DRAFT, State.PENDING_APPROVAL);
        }
        if (message instanceof Approve) {
            return transition(State.PENDING_APPROVAL, State.APPROVED);
        }
        if (message instanceof Reject) {
            return transition(State.PENDING_APPROVAL, State.REJECTED);
        }
        if (message instanceof Post) {
            return transition(State.APPROVED, State.POSTED);
        }
        if (message instanceof StateQuery) {
            return state;
        }
        return null;
    }

    private Object transition(final State expected, final State next) {
        if (state == expected) {
            state = next;
            return state;
        }
        return "ILLEGAL:" + state + "->" + next;
    }
}
