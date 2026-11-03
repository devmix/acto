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

package com.github.devmix.process.acto.examples.edtech

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class EnrollmentObjectSpec extends Specification {

    def 'an enrollment storm cannot exceed the seat cap'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def course = registry.create({ id, options -> new EnrollmentObject(id, options) }, 'course', 50)

        when: '100 students race to enroll'
        def results = (1..100).collect { course.request(new EnrollmentObject.Enroll("s$it")) }
                *.get(5, TimeUnit.SECONDS)

        then: 'exactly the seat count is enrolled'
        results.count { it == Boolean.TRUE } == 50
        course.<Integer>request(new EnrollmentObject.Enrolled()).get(5, TimeUnit.SECONDS) == 50

        and: 're-enrolling an existing student is idempotent'
        course.<Boolean>request(new EnrollmentObject.Enroll('s1')).get(5, TimeUnit.SECONDS) == Boolean.TRUE
        course.<Integer>request(new EnrollmentObject.Enrolled()).get(5, TimeUnit.SECONDS) == 50

        cleanup:
        registry.shutdown(true)
    }
}
