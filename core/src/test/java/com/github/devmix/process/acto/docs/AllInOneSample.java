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

package com.github.devmix.process.acto.docs;

import com.github.devmix.process.acto.ActiveObject;
import com.github.devmix.process.acto.ActiveObjectsDispatcher;
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher;
import com.github.devmix.process.acto.fixtures.EchoObject;
import com.github.devmix.process.acto.fixtures.EchoOptions;

import java.util.concurrent.ExecutionException;

/**
 * @author Sergey Grachev
 */
public class AllInOneSample {

    // tag::init[]
    ActiveObjectsDispatcher dispatcher = new DefaultActiveObjectsDispatcher();

    // end::init[]

    public static void main(final String[] args) {
//        var o1 = r.create(EchoObject::new, "e:1", new EchoOptions(["e:2"], null)) as DefaultActiveObjectContext
//        var o2 = r.create(EchoObject::new, "e:2", new EchoOptions(["e:3"], null)) as DefaultActiveObjectContext
//        var o3 = r.create(EchoObject::new, "e:3", new EchoOptions(null, ["e:4"])) as DefaultActiveObjectContext
//        var o4 = r.create(EchoObject::new, "e:4", new EchoOptions(null, null)) as DefaultActiveObjectContext
    }


    private void useObjectConstructor() throws ExecutionException, InterruptedException {
        // tag::useObjectConstructor[]
        ActiveObject<EchoObject> obj = dispatcher
                .create(EchoObject::new, "object-id", new EchoOptions(false, 0)); // <1>

        Long counter = obj.get(EchoObject::getCounter).get(); // <2>
        // end::useObjectConstructor[]
    }

}
