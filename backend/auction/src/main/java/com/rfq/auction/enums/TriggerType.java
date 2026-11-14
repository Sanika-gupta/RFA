package com.rfq.auction.enums;

/** What kind of bidding activity inside the trigger window extends the auction. */
public enum TriggerType {
    BID_IN_WINDOW,    // (a) any accepted bid in the last X minutes
    ANY_RANK_CHANGE,  // (b) any existing supplier's rank moves
    L1_CHANGE         // (c) the lowest bidder changes
}
