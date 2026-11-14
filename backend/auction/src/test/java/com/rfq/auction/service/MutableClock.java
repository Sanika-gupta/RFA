package com.rfq.auction.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock the test can move, so we can "be" at 5:55 PM without waiting. */
public class MutableClock extends Clock {

    private Instant now;

    public MutableClock(Instant start) { this.now = start; }

    public void set(Instant instant) { this.now = instant; }

    @Override public Instant instant() { return now; }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
}
