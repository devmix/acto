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

package com.github.devmix.process.acto.examples.erp

import com.github.devmix.process.acto.core.registry.DefaultActiveObjectsDispatcher
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author Sergey Grachev
 */
class InvoiceApprovalObjectSpec extends Specification {

    def 'the state machine accepts only legal transitions'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def invoice = registry.create({ id, options -> new InvoiceApprovalObject() }, 'inv-1', null)

        expect: 'a happy path'
        invoice.request(new InvoiceApprovalObject.Submit()).get(5, TimeUnit.SECONDS) == InvoiceApprovalObject.State.PENDING_APPROVAL
        invoice.request(new InvoiceApprovalObject.Approve('cfo')).get(5, TimeUnit.SECONDS) == InvoiceApprovalObject.State.APPROVED
        invoice.request(new InvoiceApprovalObject.Post()).get(5, TimeUnit.SECONDS) == InvoiceApprovalObject.State.POSTED

        cleanup:
        registry.shutdown(true)
    }

    def 'concurrent illegal transitions cannot corrupt the state'() {
        given:
        def registry = new DefaultActiveObjectsDispatcher()
        def invoice = registry.create({ id, options -> new InvoiceApprovalObject() }, 'inv-2', null)

        when: 'post and approve race on a draft invoice'
        def results = [invoice.request(new InvoiceApprovalObject.Post()),
                       invoice.request(new InvoiceApprovalObject.Approve('cfo'))]
                *.get(5, TimeUnit.SECONDS)

        then: 'both are rejected, the state stays DRAFT'
        results.every { it.toString().startsWith('ILLEGAL') }
        invoice.<Object>request(new InvoiceApprovalObject.StateQuery()).get(5, TimeUnit.SECONDS) == InvoiceApprovalObject.State.DRAFT

        cleanup:
        registry.shutdown(true)
    }
}
