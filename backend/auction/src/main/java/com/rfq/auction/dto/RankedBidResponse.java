package com.rfq.auction.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RankedBidResponse(
        String rank,               // "L1", "L2", ...
        Long bidId,
        String supplierName,
        BigDecimal freightCharge,
        BigDecimal originCharge,
        BigDecimal destinationCharge,
        BigDecimal total,
        int transitTime,
        int validity,
        Instant createdAt
) { }
