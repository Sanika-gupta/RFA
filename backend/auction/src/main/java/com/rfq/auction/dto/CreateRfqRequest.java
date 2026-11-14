package com.rfq.auction.dto;

import com.rfq.auction.enums.TriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Times are ISO-8601 UTC strings, e.g. "2026-11-14T18:00:00Z". */
public record CreateRfqRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull Instant startAt,
        @NotNull Instant bidCloseAt,
        @NotNull Instant forcedCloseAt,
        Instant pickupDate,
        @NotNull @Positive Integer extensionWindowMinutes,
        @NotNull @Positive Integer extensionMinutes,
        @NotNull TriggerType triggerType
) { }
