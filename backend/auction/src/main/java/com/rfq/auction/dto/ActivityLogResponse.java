package com.rfq.auction.dto;

import com.rfq.auction.enums.ActivityType;

import java.time.Instant;

public record ActivityLogResponse(
        Long id,
        ActivityType eventType,
        String reason,
        Instant oldClose,
        Instant newClose,
        boolean capped,
        Long bidId,
        Instant createdAt
) { }
