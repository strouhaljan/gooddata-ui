/*
 * Copyright (C) 2007-2017, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.cfal;

/**
 * Event writer.
 */
public interface AuditLogEventWriter extends AutoCloseable {

    /**
     * Write a single event
     * @param event event
     * @return number of characters written
     */
    int logEvent(AuditLogEvent event);

    @Override
    default void close() throws Exception {}
}
