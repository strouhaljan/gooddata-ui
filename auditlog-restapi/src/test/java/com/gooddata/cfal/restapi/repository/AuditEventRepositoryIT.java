/*
 * Copyright (C) 2007-2017, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.cfal.restapi.repository;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import com.gooddata.cfal.restapi.model.AuditEvent;
import com.gooddata.cfal.restapi.util.EntityIdMatcher;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations="classpath:application-test.properties")
public class AuditEventRepositoryIT {

    private static final String DOMAIN1 = RandomStringUtils.randomAlphabetic(10);
    private static final String DOMAIN2 = RandomStringUtils.randomAlphabetic(10);

    private static final String USER1 = RandomStringUtils.randomAlphabetic(10);
    private static final String USER2 = RandomStringUtils.randomAlphabetic(10);

    @Autowired
    private AuditLogEventRepository auditLogEventRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private AuditEvent event1;
    private AuditEvent event2;
    private AuditEvent event3;

    @Before
    public void setUp() {
        mongoTemplate.remove(new Query(), DOMAIN1);
        mongoTemplate.remove(new Query(), DOMAIN2);

        event1 = new AuditEvent(DOMAIN1, USER1, new DateTime());
        event2 = new AuditEvent(DOMAIN1, USER2, new DateTime());
        event3 = new AuditEvent(DOMAIN1, USER1, new DateTime());

        mongoTemplate.save(event1, DOMAIN1);
        mongoTemplate.save(event2, DOMAIN1);
        mongoTemplate.save(event3, DOMAIN1);
    }

    @Test
    public void testFindByDomain() {
        List<AuditEvent> events = auditLogEventRepository.findByDomain(DOMAIN1, 10, null);

        assertThat(events, is(notNullValue()));
        assertThat(events, containsInAnyOrder(EntityIdMatcher.hasSameIdAs(event1), EntityIdMatcher.hasSameIdAs(event2), EntityIdMatcher.hasSameIdAs(event3)));
    }

    @Test
    public void testFindByDomainHitPageLimit() {
        List<AuditEvent> events = auditLogEventRepository.findByDomain(DOMAIN1, 2, null);

        assertThat(events, is(notNullValue()));
        assertThat(events, containsInAnyOrder(EntityIdMatcher.hasSameIdAs(event1), EntityIdMatcher.hasSameIdAs(event2)));
    }

    @Test
    public void testFindByDomainNextPage() {
        List<AuditEvent> events = auditLogEventRepository.findByDomain(DOMAIN1, 2, event2.getId());

        assertThat(events, is(notNullValue()));
        assertThat(events, Matchers.contains(EntityIdMatcher.hasSameIdAs(event3)));
    }

    @Test
    public void testFindByDomainWithNotExistentOffset() {
        List<AuditEvent> events = auditLogEventRepository.findByDomain(DOMAIN1, 2, new ObjectId());

        assertThat(events, is(notNullValue()));
        assertThat(events, hasSize(0));
    }

    @Test
    public void testSave() {
        AuditEvent test = new AuditEvent(DOMAIN2, USER1, new DateTime());

        auditLogEventRepository.save(test);

        assertThat(test.getId(), is(notNullValue()));
    }

    @Test
    public void testDeleteAll() {
        auditLogEventRepository.save(new AuditEvent(DOMAIN2, USER2, new DateTime()));

        auditLogEventRepository.deleteAllByDomain(DOMAIN2);

        assertThat(mongoTemplate.findAll(AuditEvent.class, DOMAIN2), hasSize(0));
    }

    @Test
    public void testFindByDomainAndUser() {
        List<AuditEvent> eventsUser1 = auditLogEventRepository.findByDomainAndUser(DOMAIN1, USER1, 10, null);

        assertThat(eventsUser1, is(notNullValue()));
        assertThat(eventsUser1, containsInAnyOrder(EntityIdMatcher.hasSameIdAs(event1), EntityIdMatcher.hasSameIdAs(event3)));

        List<AuditEvent> eventsUser2 = auditLogEventRepository.findByDomainAndUser(DOMAIN1, USER2, 10, null);

        assertThat(eventsUser2, is(notNullValue()));
        assertThat(eventsUser2, Matchers.contains(EntityIdMatcher.hasSameIdAs(event2)));
    }

    @Test
    public void testFindByDomainAndUserWithOffset() {
        List<AuditEvent> eventsUser1 = auditLogEventRepository.findByDomainAndUser(DOMAIN1, USER1, 10, event1.getId());

        assertThat(eventsUser1, is(notNullValue()));
        assertThat(eventsUser1, Matchers.contains(EntityIdMatcher.hasSameIdAs(event3)));

        List<AuditEvent> eventsUser2 = auditLogEventRepository.findByDomainAndUser(DOMAIN1, USER2, 10, event2.getId());

        assertThat(eventsUser2, is(notNullValue()));
        assertThat(eventsUser2, hasSize(0));
    }

    @Test
    public void testFindByDomainAndUserMultiplePages() {
        List<AuditEvent> firstPage = auditLogEventRepository.findByDomainAndUser(DOMAIN1, USER1, 1, null);

        assertThat(firstPage, is(notNullValue()));
        assertThat(firstPage, Matchers.contains(EntityIdMatcher.hasSameIdAs(event1)));

        List<AuditEvent> secondPage = auditLogEventRepository.findByDomainAndUser(DOMAIN1, USER1, 1, event1.getId());

        assertThat(secondPage, is(notNullValue()));
        assertThat(secondPage, Matchers.contains(EntityIdMatcher.hasSameIdAs(event3)));

        List<AuditEvent> thirdPage = auditLogEventRepository.findByDomainAndUser(DOMAIN1, USER1, 1, event3.getId());

        assertThat(thirdPage, is(notNullValue()));
        assertThat(thirdPage, hasSize(0));
    }
}