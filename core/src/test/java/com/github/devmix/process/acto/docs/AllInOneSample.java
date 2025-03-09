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
