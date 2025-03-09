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
