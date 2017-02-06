/*
 * Copyright (C) 2007-2017, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.cfal.restapi.rest;

import com.gooddata.cfal.restapi.config.WebConfig;
import com.gooddata.cfal.restapi.dto.AuditEventDTO;
import com.gooddata.cfal.restapi.dto.AuditEventsDTO;
import com.gooddata.cfal.restapi.dto.RequestParameters;
import com.gooddata.cfal.restapi.exception.UserNotDomainAdminException;
import com.gooddata.cfal.restapi.exception.UserNotSpecifiedException;
import com.gooddata.cfal.restapi.exception.ValidationException;
import com.gooddata.cfal.restapi.service.AuditEventService;
import com.gooddata.cfal.restapi.service.UserDomainService;
import com.gooddata.collections.Paging;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;

import static com.gooddata.cfal.restapi.util.DateUtils.date;
import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(AuditEventController.class)
@Import(WebConfig.class)
public class AuditEventControllerTest {

    private static final String X_PUBLIC_USER_ID = "X-GDC-PUBLIC-USER-ID";

    private static final String USER_ID = "TEST_ID";

    private static final String NOT_ADMIN_USER_ID = "NOT_ADMIN";

    private static final String BAD_OFFSET = "badOffset";

    private static final String DOMAIN = "test domain";

    private static final ObjectId OFFSET = new ObjectId();

    private static final String INVALID_TIME_INTERVAL_MESSAGE = "\"to\" must be after \"before\"";

    private static final String OFFSET_AND_FROM_SPECIFIED_MESSAGE = "offset and time interval param \"from\" cannot be specified at once";

    private static final String TYPE_MISMATCH_MESSAGE = "Value \"%s\" is not valid for parameter \"%s\"";

    private static final String INVALID_OFFSET_MESSAGE = "Invalid offset \"%s\"";

    private static final String USER_NOT_SPECIFIED_MESSAGE = "User ID is not specified";

    private static final String USER_NOT_ADMIN_MESSAGE = "User is not admin";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditEventService auditEventService;

    @MockBean
    private UserDomainService userDomainService;

    private static final DateTime LOWER_BOUND = date("1990-01-01");
    private static final DateTime UPPER_BOUND = date("2005-01-01");

    private final AuditEventsDTO domainEvents = new AuditEventsDTO(
            Arrays.asList(new AuditEventDTO("123", "default", "user123", date("1993-03-09"), date("1993-03-09")),
                    new AuditEventDTO("456", "default", "user456", date("1993-03-09"), date("1993-03-09"))),
            new Paging("/gdc/audit/admin/events?offset=456&limit=" + RequestParameters.DEFAULT_LIMIT),
            new HashMap<String, String>() {{
                put("self", AuditEventDTO.ADMIN_URI);
            }});

    private final AuditEventsDTO eventsForUser = new AuditEventsDTO(
            Arrays.asList(new AuditEventDTO("123", "default", "user123", date("1993-03-09"), date("1993-03-09")),
                    new AuditEventDTO("456", "default", "user123", date("1993-03-09"), date("1993-03-09"))),
            new Paging("/gdc/audit/admin/events?offset=456&limit=" + RequestParameters.DEFAULT_LIMIT),
            new HashMap<String, String>() {{
                put("self", AuditEventDTO.USER_URI);
            }});

    private final AuditEventsDTO domainEventsWithTimeInterval = new AuditEventsDTO(
            Arrays.asList(new AuditEventDTO("123", "default", "user123", date("1993-03-09"), date("1993-03-09")),
                    new AuditEventDTO("456", "default", "user456", date("1995-03-09"), date("1995-03-09"))),
            new Paging("/gdc/audit/admin/events?to=" + UPPER_BOUND + "&offset=456&limit=100"),
            new HashMap<String, String>() {{
                put("self", AuditEventDTO.ADMIN_URI);
            }});

    @Before
    public void setUp() {
        doReturn(DOMAIN).when(userDomainService).findDomainForUser(USER_ID);
        doReturn(DOMAIN).when(userDomainService).findDomainForUser(NOT_ADMIN_USER_ID);
        doThrow(new UserNotDomainAdminException(USER_NOT_ADMIN_MESSAGE)).when(userDomainService).authorizeAdmin(NOT_ADMIN_USER_ID, DOMAIN);

        RequestParameters pageRequestWithBadOffset = new RequestParameters();
        pageRequestWithBadOffset.setOffset(BAD_OFFSET);

        RequestParameters pageRequestDefault = new RequestParameters();

        when(auditEventService.findByDomain(eq(DOMAIN), eq(pageRequestDefault))).thenReturn(domainEvents);

        when(auditEventService.findByDomainAndUser(eq(DOMAIN), eq(USER_ID), eq(pageRequestDefault))).thenReturn(eventsForUser);

        RequestParameters boundedRequestParameters = new RequestParameters();
        boundedRequestParameters.setFrom(LOWER_BOUND);
        boundedRequestParameters.setTo(UPPER_BOUND);
        when(auditEventService.findByDomain(eq(DOMAIN), eq(boundedRequestParameters))).thenReturn(domainEventsWithTimeInterval);

        RequestParameters lowerBoundRequestParameters = new RequestParameters();
        lowerBoundRequestParameters.setFrom(LOWER_BOUND);

        RequestParameters pageRequestWithOffset = new RequestParameters();
        pageRequestWithOffset.setOffset(OFFSET.toString());
        pageRequestWithOffset.setFrom(LOWER_BOUND);
    }

    @Test
    public void testListAuditEventsUserNotSpecified() throws Exception {
        mockMvc.perform(get(AuditEventDTO.ADMIN_URI))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.error.errorClass", is(UserNotSpecifiedException.class.getName())))
               .andExpect(jsonPath("$.error.message", is(USER_NOT_SPECIFIED_MESSAGE)));
    }

    @Test
    public void testListAuditEventsInvalidOffset() throws Exception {
        String errorMessage = format(INVALID_OFFSET_MESSAGE, BAD_OFFSET);

        mockMvc.perform(get(AuditEventDTO.ADMIN_URI)
                .param("offset", BAD_OFFSET)
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorClass", is(ValidationException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(errorMessage)));
    }

    @Test
    public void testListAuditEventsNotAdmin() throws Exception {
        mockMvc.perform(get(AuditEventDTO.ADMIN_URI)
                .header(X_PUBLIC_USER_ID, NOT_ADMIN_USER_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.errorClass", is(UserNotDomainAdminException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(USER_NOT_ADMIN_MESSAGE)));
    }

    @Test
    public void testListAuditEventsDefaultPaging() throws Exception {
        mockMvc.perform(get(AuditEventDTO.ADMIN_URI)
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(IOUtils.toString(getClass().getResourceAsStream("auditEvents.json"))));
    }

    @Test
    public void testListAuditEventsForUserNotSpecified() throws Exception {
        mockMvc.perform(get(AuditEventDTO.USER_URI))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorClass", is(UserNotSpecifiedException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(USER_NOT_SPECIFIED_MESSAGE)));
    }

    @Test
    public void testListAuditEventsForUserInvalidOffset() throws Exception {
        String errorMessage = format(INVALID_OFFSET_MESSAGE, BAD_OFFSET);

        mockMvc.perform(get(AuditEventDTO.USER_URI)
                .param("offset", BAD_OFFSET)
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorClass", is(ValidationException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(errorMessage)));
    }

    @Test
    public void testListAuditEventsForUserDefaultPaging() throws Exception {
        mockMvc.perform(get(AuditEventDTO.USER_URI)
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(IOUtils.toString(getClass().getResourceAsStream("userAuditEvents.json"))));
    }

    @Test
    public void testListAuditEventsWithTimeInterval() throws Exception {
        mockMvc.perform(get(AuditEventDTO.ADMIN_URI)
                .param("from", LOWER_BOUND.toString())
                .param("to", UPPER_BOUND.toString())
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(IOUtils.toString(getClass().getResourceAsStream("auditEventsWithTimeInterval.json"))));
    }

    @Test
    public void testListAuditEventsWithBadLimit() throws Exception {
        String wrongValue = "not number";
        String errorMessage = format(TYPE_MISMATCH_MESSAGE, wrongValue, "limit");

        mockMvc.perform(get(AuditEventDTO.ADMIN_URI)
                .param("limit", wrongValue)
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorClass", is(ValidationException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(errorMessage)));
    }

    @Test
    public void testListAuditEventsForUserWithBadLimit() throws Exception {
        String wrongValue = "not number";
        String errorMessage = format(TYPE_MISMATCH_MESSAGE, wrongValue, "limit");

        mockMvc.perform(get(AuditEventDTO.USER_URI)
                .param("limit", wrongValue)
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorClass", is(ValidationException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(errorMessage)));
    }

    @Test
    public void testListAuditEventsInvalidFrom() throws Exception {
        String wrongValue = "a";
        String errorMessage = format(TYPE_MISMATCH_MESSAGE, wrongValue, "from");

        mockMvc.perform(get(AuditEventDTO.ADMIN_URI)
                .param("from", wrongValue)
                .param("to", UPPER_BOUND.toString())
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorClass", is(ValidationException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(errorMessage)));
    }

    @Test
    public void testListAuditEventsInvalidTo() throws Exception {
        String wrongValue = "a";
        String errorMessage = format(TYPE_MISMATCH_MESSAGE, wrongValue, "to");

        mockMvc.perform(get(AuditEventDTO.ADMIN_URI)
                .param("from", LOWER_BOUND.toString())
                .param("to", wrongValue)
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorClass", is(ValidationException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(errorMessage)));
    }

    @Test
    public void testListAuditEventsForUserInvalidFrom() throws Exception {
        String wrongValue = "a";
        String errorMessage = format(TYPE_MISMATCH_MESSAGE, wrongValue, "from");

        mockMvc.perform(get(AuditEventDTO.USER_URI)
                .param("from", wrongValue)
                .param("to", UPPER_BOUND.toString())
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorClass", is(ValidationException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(errorMessage)));
    }

    @Test
    public void testListAuditEventsForUserInvalidTo() throws Exception {
        String wrongValue = "a";
        String errorMessage = format(TYPE_MISMATCH_MESSAGE, wrongValue, "to");

        mockMvc.perform(get(AuditEventDTO.USER_URI)
                .param("from", LOWER_BOUND.toString())
                .param("to", wrongValue)
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorClass", is(ValidationException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(errorMessage)));
    }

    @Test
    public void testListAuditEventsExpectedContentType() throws Exception {
        mockMvc.perform(get(AuditEventDTO.ADMIN_URI)
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", APPLICATION_JSON_VALUE + ";charset=UTF-8"));
    }

    @Test
    public void testListAuditEventsForUserExpectedContentType() throws Exception {
        mockMvc.perform(get(AuditEventDTO.USER_URI)
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", APPLICATION_JSON_VALUE + ";charset=UTF-8"));
    }

    @Test
    public void testListAuditEventsFromAndOffsetSpecified() throws Exception {
        mockMvc.perform(get(AuditEventDTO.ADMIN_URI)
                .param("offset", OFFSET.toString())
                .param("from", LOWER_BOUND.toString())
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorClass", is(ValidationException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(OFFSET_AND_FROM_SPECIFIED_MESSAGE)));
    }

    @Test
    public void testListAuditEventsForUserFromAndOffsetSpecified() throws Exception {
        mockMvc.perform(get(AuditEventDTO.USER_URI)
                .param("offset", OFFSET.toString())
                .param("from", LOWER_BOUND.toString())
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorClass", is(ValidationException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(OFFSET_AND_FROM_SPECIFIED_MESSAGE)));
    }

    @Test
    public void testListAuditEventsForUserInvalidTimeInterval() throws Exception {
        mockMvc.perform(get(AuditEventDTO.USER_URI)
                .param("from", UPPER_BOUND.toString())
                .param("to", LOWER_BOUND.toString())
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorClass", is(ValidationException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(INVALID_TIME_INTERVAL_MESSAGE)));
    }

    @Test
    public void testListAuditEventsInvalidTimeInterval() throws Exception {
        mockMvc.perform(get(AuditEventDTO.ADMIN_URI)
                .param("from", UPPER_BOUND.toString())
                .param("to", LOWER_BOUND.toString())
                .header(X_PUBLIC_USER_ID, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorClass", is(ValidationException.class.getName())))
                .andExpect(jsonPath("$.error.message", is(INVALID_TIME_INTERVAL_MESSAGE)));
    }
}
