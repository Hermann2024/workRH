package com.workrh.common.events;

import java.time.Instant;

public interface DomainEvent {
    String tenantId();
    Instant occurredAt();
    String type();
}
