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

package com.github.devmix.process.acto

import com.github.devmix.process.acto.core.context.DefaultActiveObjectContext
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import com.github.devmix.process.acto.fixtures.EchoObject
import com.github.devmix.process.acto.fixtures.EchoOptions
import com.github.devmix.process.acto.messages.Start
import com.github.devmix.process.acto.messages.Stop
import spock.lang.Specification

import java.util.concurrent.TimeUnit

import static ActiveObjectStatus.ACTIVATED
import static ActiveObjectStatus.CREATED
import static ActiveObjectStatus.DEACTIVATED
import static ActiveObjectStatus.STARTED
import static ActiveObjectStatus.STOPPED
import static ActiveObjectsTestUtils.allWithStatus
import static com.github.devmix.process.acto.ActiveObjectDependency.Type.DEPENDS_ON
import static com.github.devmix.process.acto.ActiveObjectDependency.Type.REQUIRED_FOR

/**
 * @author Sergey Grachev
 */
class ActiveObjectFactoryTest extends Specification {

    def 'start'() {
        given:
        def r = new DefaultActiveObjectsDispatcher()

        when:
        def o1 = r.create(EchoObject::new, "e:1", new EchoOptions(["e:2"], null)) as DefaultActiveObjectContext
        def o2 = r.create(EchoObject::new, "e:2", new EchoOptions(["e:3"], null)) as DefaultActiveObjectContext
        def o3 = r.create(EchoObject::new, "e:3", new EchoOptions(null, ["e:4"])) as DefaultActiveObjectContext
        def o4 = r.create(EchoObject::new, "e:4", new EchoOptions(null, null)) as DefaultActiveObjectContext

        then:
        allWithStatus(CREATED, o1, o2, o3, o4)

        when:
        o1.request(Start.standard()).get()

        then:
        allWithStatus(STARTED, o1, o2, o3, o4)

        when: 'get o1 dependencies'
        def dependsOn = o1.findDependsOn('e:2') as ActiveObjectDependency
        def requiredFor = null as ActiveObjectDependency

        then: 'check o1 dependencies'
        dependsOn.resolved
        !dependsOn.optional
        dependsOn.object != null
        dependsOn.type == DEPENDS_ON

        requiredFor == null

        when: 'get o2 dependencies'
        dependsOn = o2.findDependsOn('e:3')
        requiredFor = o2.findRequiredFor('e:1')

        then: 'check o2 dependencies'
        dependsOn.resolved
        !dependsOn.optional
        dependsOn.object != null
        dependsOn.type == DEPENDS_ON

        requiredFor.resolved
        !requiredFor.optional
        requiredFor.object != null
        requiredFor.type == REQUIRED_FOR

        when: 'get o3 dependencies'
        dependsOn = o3.findDependsOn('e:4')
        requiredFor = o3.findRequiredFor('e:2')

        then: 'check o3 dependencies'
        dependsOn.resolved
        dependsOn.optional
        dependsOn.object != null
        dependsOn.type == DEPENDS_ON

        requiredFor.resolved
        !requiredFor.optional
        requiredFor.object != null
        requiredFor.type == REQUIRED_FOR

        when: 'get o4 dependencies'
        requiredFor = o4.findRequiredFor('e:3')

        then: 'check o4 dependencies'
        requiredFor.resolved
        requiredFor.optional
        requiredFor.object != null
        requiredFor.type == REQUIRED_FOR

        cleanup:
        r.shutdown(true)
    }

    def 'stop with force'() {
        given:
        def r = new DefaultActiveObjectsDispatcher()

        def o1 = r.create(EchoObject::new, "e:1", new EchoOptions(["e:2"], null)) as DefaultActiveObjectContext
        def o2 = r.create(EchoObject::new, "e:2", new EchoOptions(["e:3"], null)) as DefaultActiveObjectContext
        def o3 = r.create(EchoObject::new, "e:3", new EchoOptions(null, ["e:4"])) as DefaultActiveObjectContext
        def o4 = r.create(EchoObject::new, "e:4", new EchoOptions(null, null)) as DefaultActiveObjectContext

        when: 'start o1'
        o1.request(Start.standard()).get()
        then:
        allWithStatus(STARTED, o1, o2, o3, o4)
        o2.findRequiredFor('e:1') != null
        o3.findRequiredFor('e:2') != null
        o4.findRequiredFor('e:3') != null

        when: 'stop o4'
        o4.request(Stop.force()).get()
        then:
        allWithStatus(STOPPED, o1, o2, o3, o4)
        o2.findRequiredFor('e:1') == null
        o3.findRequiredFor('e:2') == null
        o4.findRequiredFor('e:3') == null

        when: 'restart o1'
        o1.request(Start.standard()).get()
        then:
        allWithStatus(STARTED, o1, o2, o3, o4)
        o2.findRequiredFor('e:1') != null
        o3.findRequiredFor('e:2') != null
        o4.findRequiredFor('e:3') != null

        when: 'stop o2'
        o2.request(Stop.force()).get()
        then:
        allWithStatus(STOPPED, o1, o2)
        allWithStatus(STARTED, o3, o4)
        o2.findRequiredFor('e:1') == null
        o3.findRequiredFor('e:2') == null
        o4.findRequiredFor('e:3') != null

        cleanup:
        r.shutdown(true)
    }

    def 'transition between activate and deactivate state'() {
        given:
        def r = new DefaultActiveObjectsDispatcher(10)
        def o1 = r.create(EchoObject::new, "e:1", new EchoOptions(true, TimeUnit.MILLISECONDS.toNanos(100))) as DefaultActiveObjectContext

        when: 'send first message'
        o1.get(EchoObject::getCounter).get()
        then: 'object becomes active'
        o1.status == ACTIVATED

        when: 'we wait of long period of time'
        Thread.sleep(300)
        then: 'object becomes inactive'
        o1.status == DEACTIVATED

        when: 'send another message'
        o1.set(EchoObject::setIdleTimeout, TimeUnit.MINUTES.toNanos(5)).get()
        then: 'object becomes active again'
        o1.status == ACTIVATED

        cleanup:
        r.shutdown(true)
    }
}
