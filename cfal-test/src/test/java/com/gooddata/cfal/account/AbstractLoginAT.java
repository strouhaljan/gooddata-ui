/*
 * Copyright (C) 2007-2017, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.cfal.account;

import com.gooddata.cfal.AbstractAT;
import com.gooddata.cfal.restapi.dto.AuditEventDTO;

import java.util.List;
import java.util.function.Predicate;

public class AbstractLoginAT extends AbstractAT {

    protected static final String MESSAGE_TYPE = "LOGIN";

    private static final String LOGIN_TYPE = "loginType";
    private static final String COMPONENT = "component";
    private static final String WEBAPP = "WEBAPP";

    protected Predicate<List<AuditEventDTO>> pageCheckPredicate(final boolean success,
                                                                final String loginType) {
        return (auditEvents) -> auditEvents.stream().anyMatch(e ->
                e.getUserLogin().equals(getAccount().getLogin()) &&
                        e.getType().equals(MESSAGE_TYPE) &&
                        e.isSuccess() == success &&
                        loginType.equals(e.getParams().get(LOGIN_TYPE)) &&
                        WEBAPP.equals(e.getParams().get(COMPONENT)));
    }
}