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

package com.github.devmix.process.acto.examples.holder

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class FileWriterHolderSpec extends Specification {

    @TempDir
    Path dir

    def 'concurrent appends to a single BufferedWriter without a single synchronized'() {
        given:
        def file = dir.resolve('holders.log')
        def registry = new DefaultActiveObjectsDispatcher()
        def holder = registry.create({ id, options -> new FileWriterHolder(id, options) }, 'writer', file)

        when: '8 producers append 50 lines each'
        def producers = (0..7).collect { p ->
            Thread.start { 50.times { i -> holder.requestAndForget(new FileWriterHolder.Append("p$p-$i")) } }
        }
        producers.each { it.join() }
        holder.awaitEmptyQueue(30, TimeUnit.SECONDS)

        then: 'every line landed in the file exactly once'
        def lines = holder.<List<String>>request(new FileWriterHolder.ReadAll()).get(5, TimeUnit.SECONDS)
        lines.size() == 400
        lines.toSet().size() == 400

        and: 'destroy closes the underlying writer'
        registry.destroy('writer', false)
        Files.exists(file)

        cleanup:
        registry.shutdown(true)
    }
}
