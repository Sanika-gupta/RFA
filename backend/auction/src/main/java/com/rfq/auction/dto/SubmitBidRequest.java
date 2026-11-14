package com.rfq.auction.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SubmitBidRequest(
        @NotBlank @Size(max = 150) String supplierName,   // carrier name
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal freightCharge,
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal originCharge,
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal destinationCharge,
        @NotNull @Positive Integer transitTime,           // days
        @NotNull @Positive Integer validity               // days
) { }
