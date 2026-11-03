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

import com.github.devmix.process.acto.ActiveObject;
import com.github.devmix.process.acto.ActiveObjectContext;
import com.github.devmix.process.acto.ActiveObjectsDispatcher;
import com.github.devmix.process.acto.core.config.DispatcherConfig;
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher;
import com.github.devmix.process.acto.listeners.OnMessageListener;

import java.util.concurrent.TimeUnit;

/**
 * JPMS smoke test: compiled and run with {@code --module-path}, exercises the module graph
 * (api + core + automatic modules) end-to-end. Exits with a non-zero code on any failure.
 *
 * @author Sergey Grachev
 */
public final class ModuleSmoke {

    public static void main(final String[] args) throws Exception {
        final ActiveObjectsDispatcher dispatcher = new DefaultActiveObjectsDispatcher(DispatcherConfig.standard());
        final ActiveObject<PingObject> object = dispatcher.create((id, options) -> new PingObject(), "smoke", null);
        final String reply = object.<String>request("ping").get(10, TimeUnit.SECONDS);
        if (!"pong".equals(reply)) {
            throw new IllegalStateException("expected pong, got " + reply);
        }
        if (!dispatcher.destroy(object.getId(), false)) {
            throw new IllegalStateException("destroy failed");
        }
        dispatcher.shutdown(false);
        System.out.println("JPMS smoke OK");
    }

    private ModuleSmoke() {
    }

    private static final class PingObject implements OnMessageListener<PingObject> {

        @Override
        public Object onObjectMessage(final Object message, final ActiveObjectContext<PingObject> context) {
            return "ping".equals(message) ? "pong" : null;
        }
    }
}
