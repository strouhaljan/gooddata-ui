/*
 * Copyright (C) 2007-2017, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.cfal.restapi.util;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

import com.gooddata.auditevent.AuditEvent;
import com.gooddata.auditevent.AuditEvents;
import com.gooddata.auditevent.AuditEventPageRequest;
import com.gooddata.cfal.restapi.model.AuditEventEntity;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConversionUtilsTest {

    private static final String BASE_URI = "uri";

    private static final ObjectId ID = new ObjectId();
    private static final String DOMAIN = "domain";
    private static final String USER_LOGIN = "bear@goddata.com";
    private static final DateTime TIME = new DateTime();
    private static final String IP = "127.0.0.1";
    private static final boolean SUCCESS = true;
    private static final String TYPE = "login";
    private static final Map<String, String> EMPTY_PARAMS = new HashMap<>();
    private static final Map<String, String> EMPTY_LINKS = new HashMap<>();

    @Test
    public void testCreateAuditEventDTO() {
        AuditEvent auditEventDTO = ConversionUtils.createAuditEventDTO(new AuditEventEntity(ID, DOMAIN, USER_LOGIN, TIME, IP, SUCCESS, TYPE, EMPTY_PARAMS, EMPTY_LINKS));

        assertThat(auditEventDTO.getId(), is(ID.toString()));
        assertThat(auditEventDTO.getUserLogin(), is(USER_LOGIN));
        assertThat(auditEventDTO.getOccurred(), is(TIME));
        assertThat(auditEventDTO.getRecorded(), is(new DateTime(ID.getDate(), DateTimeZone.UTC)));
    }

    @Test(expected = NullPointerException.class)
    public void testCreateAuditEventDTOnullValue() {
        ConversionUtils.createAuditEventDTO(null);
    }

    @Test
    public void testCreateAuditEventsDTO() {
        AuditEventPageRequest requestParameters = new AuditEventPageRequest();
        AuditEvents auditEventsDTO = ConversionUtils.createAuditEventsDTO(
                BASE_URI, Collections.singletonList(new AuditEventEntity(ID, DOMAIN, USER_LOGIN, TIME, IP, SUCCESS, TYPE, EMPTY_PARAMS, EMPTY_LINKS)), requestParameters);

        assertThat(auditEventsDTO, hasSize(1));
        assertThat(auditEventsDTO.getPaging().getNextUri(), is(nullValue()));
    }

    @Test
    public void testCreateAuditEventsDTOWithTimeParameterTo() {
        AuditEventPageRequest requestParameters = new AuditEventPageRequest();
        requestParameters.setTo(TIME);
        AuditEvents auditEventsDTO = ConversionUtils.createAuditEventsDTO(
                BASE_URI, Collections.singletonList(new AuditEventEntity(ID, DOMAIN, USER_LOGIN, TIME, IP, SUCCESS, TYPE, EMPTY_PARAMS, EMPTY_LINKS)), requestParameters);

        assertThat(auditEventsDTO, hasSize(1));
        assertThat(auditEventsDTO.getPaging().getNextUri(), is(nullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void testCreateAuditEventsDTOnullList() {
        ConversionUtils.createAuditEventsDTO(BASE_URI, null, new AuditEventPageRequest());
    }

    @Test(expected = NullPointerException.class)
    public void testCreateAuditEventsDTOnullUri() {
        ConversionUtils.createAuditEventsDTO(null, new ArrayList<>(), new AuditEventPageRequest());
    }

    @Test(expected = NullPointerException.class)
    public void testCreateAuditEventsDTOnullRequestParameters() {
        ConversionUtils.createAuditEventsDTO(BASE_URI, new ArrayList<>(), null);
    }

    @Test
    public void testCreateAuditEventsDTOemptyList() {
        AuditEvents auditEventsDTO = ConversionUtils.createAuditEventsDTO(BASE_URI, Collections.emptyList(), new AuditEventPageRequest());

        assertThat(auditEventsDTO.getPaging().getNextUri(), is(nullValue()));
    }

    @Test
    public void testCreateAuditEventsDTOListHasMoreElementsThanLimit() {
        AuditEventEntity event = new AuditEventEntity(ID, DOMAIN, USER_LOGIN, TIME, IP, SUCCESS, TYPE, EMPTY_PARAMS, EMPTY_LINKS);

        AuditEventPageRequest requestParameters = new AuditEventPageRequest();
        requestParameters.setLimit(3);

        AuditEvents auditEventsDTO = ConversionUtils.createAuditEventsDTO(BASE_URI, asList(event, event, event, event), requestParameters);

        assertThat(auditEventsDTO, hasSize(3));
        assertThat(auditEventsDTO.getPaging().getNextUri(), is(notNullValue()));
    }

    @Test
    public void testCreateAuditEventsDTOListHasExactlyElementsOfLimit() {
        AuditEventEntity event = new AuditEventEntity(ID, DOMAIN, USER_LOGIN, TIME, IP, SUCCESS, TYPE, EMPTY_PARAMS, EMPTY_LINKS);

        AuditEventPageRequest requestParameters = new AuditEventPageRequest();
        requestParameters.setLimit(3);

        AuditEvents auditEventsDTO = ConversionUtils.createAuditEventsDTO(BASE_URI, asList(event, event, event), requestParameters);

        assertThat(auditEventsDTO, hasSize(3));
        assertThat(auditEventsDTO.getPaging().getNextUri(), is(nullValue()));
    }
}
