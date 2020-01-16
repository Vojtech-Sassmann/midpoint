/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.StateOfModule;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationActionValue;
import com.evolveum.midpoint.web.security.module.authentication.HttpModuleAuthentication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.security.api.AuthorizationConstants.AUTZ_UI_TASK_DETAIL_URL;

/**
 * @author skublik
 */

public class MidpointHttpAuthorizationEvaluator extends MidPointGuiAuthorizationEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointHttpAuthorizationEvaluator.class);

    public static final String CLASS_DOT = MidpointHttpAuthorizationEvaluator.class.getName() + ".";

    public static final String OPERATION_REST_SERVICE = CLASS_DOT + "restService";

    private ModelService model;
    private TaskManager taskManager;
    private SecurityContextManager securityContextManager;

    public MidpointHttpAuthorizationEvaluator(SecurityEnforcer securityEnforcer, SecurityContextManager securityContextManager,
                                              TaskManager taskManager, ModelService model) {
        super(securityEnforcer, securityContextManager, taskManager);
        this.model = model;
        this.taskManager = taskManager;
        this.securityContextManager = securityContextManager;
    }

    @Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes) throws AccessDeniedException, InsufficientAuthenticationException {
        super.decide(authentication, object, configAttributes);
        if (authentication instanceof MidpointAuthentication) {
            for (ModuleAuthentication moduleAuthentication : ((MidpointAuthentication) authentication).getAuthentications()) {
                if (StateOfModule.SUCCESSFULLY.equals(moduleAuthentication.getState())
                    && moduleAuthentication instanceof HttpModuleAuthentication
                    && ((HttpModuleAuthentication) moduleAuthentication).getProxyUserOid() != null) {
                    String oid = ((HttpModuleAuthentication) moduleAuthentication).getProxyUserOid();
                    Task task = taskManager.createTaskInstance(OPERATION_REST_SERVICE);
                    task.setChannel(SchemaConstants.CHANNEL_REST_URI);
                    List<String> requiredActions = new ArrayList<>();
                    PrismObject<UserType> authorizedUser = searchUser(oid, task);
                    try {
                        if (authorizedUser == null) {
                            SystemException e = new SystemException("Couldn't get proxy user");
                            throw e;
                        }
                        task.setOwner(authorizedUser);

                        requiredActions.add(AuthorizationConstants.AUTZ_REST_PROXY_URL);

                        MidPointPrincipal principal= securityContextManager.getUserProfileService().getPrincipal(authorizedUser);

                        decideInternal(principal, requiredActions, authentication, object, task);

                        ((MidpointAuthentication) authentication).setPrincipal(principal);
                    } catch (SystemException | SchemaException | CommunicationException | ConfigurationException
                            | SecurityViolationException | ExpressionEvaluationException e) {
                        LOGGER.error("Error while processing authorization: {}", e.getMessage(), e);
                        LOGGER.trace("DECIDE: authentication={}, object={}, requiredActions={}: ERROR {}",
                                authentication, object, requiredActions, e.getMessage());
                        throw new SystemException("Error while processing authorization: " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    private PrismObject<UserType> searchUser(String oid, Task task) {
        return securityContextManager.runPrivileged(new Producer<PrismObject<UserType>>() {
            @Override
            public PrismObject<UserType> run() {
                PrismObject<UserType> user;
                try {
                    user = model.getObject(UserType.class, oid, null, task, task.getResult());
                } catch (SchemaException | ObjectNotFoundException | SecurityViolationException
                        | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                    return null;
                }
                return user;

            }
        });

    }
}