/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.channel;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.forgetpassword.PageResetPassword;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author skublik
 */

public class RestAuthenticationChannel extends AuthenticationChannelImpl {


    public RestAuthenticationChannel(AuthenticationSequenceChannelType channel) {
        super(channel);
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_REST_URI;
    }

    public String getPathAfterSuccessfulAuthentication() {
        return "/ws/rest/self";
    }

//    public Collection<Authorization> resolveAuthorities(Collection<Authorization> authorities) {
//        ArrayList<Authorization> newAuthorities = new ArrayList<Authorization>();
//        for (GrantedAuthority authority : authorities) {
//            List<String> authoritiesString = new ArrayList<String>();
//                Authorization clone = ((Authorization) authority).clone();
//                authoritiesString = clone.getAction();
//                List<String> newAction = new ArrayList<String>();
//                for (String authorityString : authoritiesString) {
//                    if (authorityString.startsWith(AuthorizationConstants.NS_AUTHORIZATION_REST)
//                    || authorityString.equals(AuthorizationConstants.AUTZ_ALL_URL)) {
//                        newAction.add(authorityString);
//                    }
//                }
//                if (!newAction.isEmpty()) {
//                    clone.getAction().clear();
//                    clone.getAction().addAll(newAction);
//                    newAuthorities.add(clone);
//                }
//        }
//        return newAuthorities;
//    }
}
