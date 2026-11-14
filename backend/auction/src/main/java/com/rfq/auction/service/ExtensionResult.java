package com.rfq.auction.service;

import java.time.Instant;

/** Outcome of the extension check for one bid. */
public record ExtensionResult(boolean extended, Instant oldClose, Instant newClose, boolean capped) {

    public static ExtensionResult none(Instant currentClose) {
        return new ExtensionResult(false, currentClose, currentClose, false);
    }
}
