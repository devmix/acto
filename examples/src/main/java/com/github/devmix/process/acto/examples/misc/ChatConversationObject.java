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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A chat conversation: message sequence numbers are assigned in mailbox order, so every
 * client sees the same total order of messages regardless of producer concurrency.
 *
 * @author Sergey Grachev
 */
public class ChatConversationObject implements OnMessageListener<ChatConversationObject> {

    public record Send(String user, String text) {
    }

    public record History() {
    }

    private final List<String> messages = new ArrayList<>();

    @Override
    public Object onObjectMessage(final Object message, final ActiveObjectContext<ChatConversationObject> context) {
        if (message instanceof Send send) {
            messages.add(messages.size() + 1 + " " + send.user() + ": " + send.text());
            return messages.size();
        }
        if (message instanceof History) {
            return List.copyOf(messages);
        }
        return null;
    }
}
