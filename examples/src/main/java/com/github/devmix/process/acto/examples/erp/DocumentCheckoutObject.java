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
import com.github.devmix.process.acto.messages.Idle;
import com.github.devmix.process.acto.listeners.OnIdleListener;
import com.github.devmix.process.acto.listeners.OnMessageListener;

/**
 * ECM document checkout: exactly one editor holds the document at a time, and a forgotten
 * checkout is released automatically when the heartbeat deactivates the object after the
 * idle timeout. The lock state is plain fields — the mailbox serializes every access.
 *
 * @author Sergey Grachev
 */
public class DocumentCheckoutObject implements OnMessageListener<DocumentCheckoutObject>,
        OnIdleListener<DocumentCheckoutObject> {

    public record Checkout(String user) {
    }

    public record Checkin(String user) {
    }

    public record Holder() {
    }

    public record AutoReleases() {
    }

    private String holder;
    private int autoReleases;

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<DocumentCheckoutObject> context) {
        if (message instanceof Checkout checkout) {
            if (holder == null || holder.equals(checkout.user())) {
                holder = checkout.user();
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if (message instanceof Checkin checkin) {
            if (checkin.user().equals(holder)) {
                holder = null;
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if (message instanceof Holder) {
            return holder;
        }
        if (message instanceof AutoReleases) {
            return autoReleases;
        }
        return null;
    }

    @Override
    public boolean onObjectActivate(final Idle.Activate message, final ActiveObjectContext<DocumentCheckoutObject> context) {
        return true;
    }

    @Override
    public boolean onObjectDeactivate(final Idle.Deactivate message, final ActiveObjectContext<DocumentCheckoutObject> context) {
        if (holder != null) {
            holder = null;
            autoReleases++;
        }
        return true;
    }
}
