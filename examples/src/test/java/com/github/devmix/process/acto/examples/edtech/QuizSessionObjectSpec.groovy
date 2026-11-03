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

import com.github.devmix.process.acto.core.config.DispatcherConfig
import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class QuizSessionObjectSpec extends Specification {

    def 'a silent student is auto-submitted by the heartbeat'() {
        given:
        def config = DispatcherConfig.builder()
                .heartbeatInterval(Duration.ofMillis(20))
                .idleTimeout(Duration.ofMillis(100))
                .build()
        def registry = new DefaultActiveObjectsDispatcher(config)
        def quiz = registry.create({ id, options -> new QuizSessionObject() }, 'quiz', null)

        when: 'the student answers one question and goes silent'
        quiz.request(new QuizSessionObject.Answer(1, 'B')).get(5, TimeUnit.SECONDS)
        Thread.sleep(500)

        then: 'the attempt was auto-submitted'
        quiz.<String>request(new QuizSessionObject.State()).get(5, TimeUnit.SECONDS) == 'SUBMITTED'
        quiz.<List<String>>request(new QuizSessionObject.Answers()).get(5, TimeUnit.SECONDS) == ['1:B']

        cleanup:
        registry.shutdown(true)
    }
}
