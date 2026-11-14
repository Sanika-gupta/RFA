package com.rfq.auction.enums;

/** Never stored: computed from the RFQ's times and the current server time. */
public enum RfqStatus {
    UPCOMING,
    ACTIVE,
    CLOSED,
    FORCE_CLOSED
}
