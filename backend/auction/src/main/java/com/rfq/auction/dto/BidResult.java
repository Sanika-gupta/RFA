package com.rfq.auction.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** What the bidder gets back: their bid, their rank, and whether it extended the auction. */
public record BidResult(
        Long bidId,
        String supplierName,
        BigDecimal total,
        String rank,
        boolean extended,
        Instant bidCloseAt
) { }
