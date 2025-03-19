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

package com.github.devmix.process.acto.utils

import com.github.devmix.process.acto.ActiveObject
import com.github.devmix.process.acto.core.utils.ObjectInfoUtils
import spock.lang.Specification


/**
 * @author Sergey Grachev
 */
class ObjectInfoUtilsTest extends Specification {

    def "UnrollChain"() {
        given:
        def o1 = Mock(ActiveObject)
        def o2 = Mock(ActiveObject)
        def o3 = Mock(ActiveObject)

        o1.id >> '1'
        o2.id >> '2'
        o3.id >> '3'

        when:
        def chain = ObjectInfoUtils.unrollChain([o1, o2, o3])

        then:
        chain == '[1 > 2 > 3]'
    }
}
