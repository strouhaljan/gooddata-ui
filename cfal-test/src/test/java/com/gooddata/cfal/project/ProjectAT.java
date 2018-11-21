/*
 * Copyright (C) 2007-2017, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.cfal.project;

import com.gooddata.cfal.AbstractAT;
import com.gooddata.auditevent.AuditEvent;
import com.gooddata.project.Invitation;
import com.gooddata.project.Project;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.function.Predicate;

public class ProjectAT extends AbstractAT {

    private static final String MESSAGE_TYPE = "INVITATION_SENT";
    private final String email = "qa+" + RandomStringUtils.randomAlphanumeric(10) + "@gooddata.com";

    @BeforeClass(groups = MESSAGE_TYPE)
    public void sendInvitation() throws Exception {
        final Invitation invitation = new Invitation(email);
        final Project project = projectHelper.getOrCreateProject();
        gd.getProjectService().sendInvitations(project, invitation);
    }

    @Test(groups = MESSAGE_TYPE)
    public void testAddUserEventUserApi() throws Exception {
        doTestUserApi(eventCheck(MESSAGE_TYPE), MESSAGE_TYPE);
    }

    @Test(groups = MESSAGE_TYPE)
    public void testAddUserEventAdminApi() throws Exception {
        doTestAdminApi(eventCheck(MESSAGE_TYPE), MESSAGE_TYPE);
    }

    private Predicate<AuditEvent> eventCheck(final String messageType) {
        final Project project = projectHelper.getOrCreateProject();
        return (e ->
                        getAccount().getLogin().equals(e.getUserLogin()) &&
                        messageType.equals(e.getType()) &&
                        e.isSuccess() &&
                        email.equals(e.getParams().get("invited")) &&
                        project.getUri().equals(e.getLinks().get("project"))
                );
    }

}