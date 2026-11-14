package com.rfq.auction.dto;

import com.rfq.auction.enums.RfqStatus;
import com.rfq.auction.enums.TriggerType;

import java.math.BigDecimal;
import java.time.Instant;

public record RfqResponse(
        Long id,
        String name,
        Instant startAt,
        Instant bidCloseAt,
        Instant forcedCloseAt,
        Instant pickupDate,
        int extensionWindowMinutes,
        int extensionMinutes,
        TriggerType triggerType,
        RfqStatus status,
        BigDecimal lowestBid,      // null if no bids yet
        String l1Supplier,         // null if no bids yet
        int bidCount,
        Instant createdAt
) { }
