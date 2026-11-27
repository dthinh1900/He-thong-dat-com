package com.mealbooking.backend.domain.enums;

public enum TicketStatus {
    PENDING_APPROVAL, AVAILABLE, LISTED_FOR_EXCHANGE, USED, CANCELLED, REJECTED, WITHDRAWN;

    public boolean canReregister() {
        return this == CANCELLED || this == WITHDRAWN;
    }

    public boolean isBillable() {
        return this == AVAILABLE || this == LISTED_FOR_EXCHANGE || this == USED;
    }
}
