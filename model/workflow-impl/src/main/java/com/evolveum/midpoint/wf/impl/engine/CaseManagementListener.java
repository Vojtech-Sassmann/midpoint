/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine;

import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcher;
import com.evolveum.midpoint.casemgmt.api.CaseEventListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Listener that accepts case creation events generated by lower layers (UCF).
 * In the future it could pass them to WorkflowEngine to be processed
 *
 * @author mederly
 */
@Component
public class CaseManagementListener  {

    private static final Trace LOGGER = TraceManager.getTrace(CaseManagementListener.class);

    //private static final String DOT_CLASS = WorkflowListener.class.getName() + ".";


//    @Override
//    public void onWorkItemCreation(CaseWorkItemType workItem, CaseType aCase, Task task, OperationResult result) {
//        for (ObjectReferenceType assigneeRef : workItem.getAssigneeRef()) {
//            WorkItemEvent event = new WorkItemEvent(identifierGenerator, ChangeType.ADD, workItem,
//                    SimpleObjectRefImpl.create(functions, assigneeRef), aCase);
//            processEvent(event, result);
//        }
//    }
//
//    private void processEvent(CaseWorkItemEvent event, OperationResult result) {
//        try {
//            notificationManager.processEvent(event);
//        } catch (RuntimeException e) {
//            result.recordFatalError("An unexpected exception occurred when preparing and sending notifications: " + e.getMessage(), e);
//            LoggingUtils.logUnexpectedException(LOGGER, "An unexpected exception occurred when preparing and sending notifications: " + e.getMessage(), e);
//        }
//
//        // todo work correctly with operationResult (in whole notification module)
//        if (result.isUnknown()) {
//            result.computeStatus();
//        }
//        result.recordSuccessIfUnknown();
//    }


}
