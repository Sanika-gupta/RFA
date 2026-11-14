package com.rfq.auction.service;

import com.rfq.auction.dto.*;
import com.rfq.auction.enums.ActivityType;
import com.rfq.auction.enums.RfqStatus;
import com.rfq.auction.enums.TriggerType;
import com.rfq.auction.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The spec's example, end to end through the services and a real (in-memory) database:
 * close 18:00, forced 18:20, X = 10 min, Y = 5 min.
 */
@SpringBootTest
class AuctionServiceTest {

    static final Instant START = Instant.parse("2026-11-14T17:00:00Z");
    static final Instant CLOSE = Instant.parse("2026-11-14T18:00:00Z");
    static final Instant FORCED = Instant.parse("2026-11-14T18:20:00Z");

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() { return new MutableClock(Instant.parse("2026-11-14T16:00:00Z")); }
    }

    @Autowired MutableClock clock;
    @Autowired RfqService rfqService;
    @Autowired AuctionService auctionService;

    // ---------------------------------------------------------------- helpers

    private Long rfq(TriggerType type) {
        return rfq(type, CLOSE);
    }

    private Long rfq(TriggerType type, Instant close) {
        clock.set(Instant.parse("2026-11-14T16:00:00Z"));
        return rfqService.create(new CreateRfqRequest("Test RFQ", START, close, FORCED, null, 10, 5, type)).id();
    }

    private BidResult bid(Long rfqId, String hhmm, String supplier, int total) {
        clock.set(Instant.parse("2026-11-14T" + hhmm + ":00Z"));
        return auctionService.submitBid(rfqId, new SubmitBidRequest(
                supplier, BigDecimal.valueOf(total), BigDecimal.ZERO, BigDecimal.ZERO, 3, 30));
    }

    private Instant closeOf(Long rfqId) {
        return rfqService.get(rfqId).bidCloseAt();
    }

    private static Instant t(String hhmm) {
        return Instant.parse("2026-11-14T" + hhmm + ":00Z");
    }

    // ---------------------------------------------------------------- RFQ validation

    @Test
    void forcedCloseMustBeAfterBidClose() {
        assertThrows(BadRequestException.class, () -> rfqService.create(
                new CreateRfqRequest("x", START, CLOSE, CLOSE, null, 10, 5, TriggerType.BID_IN_WINDOW)));
    }

    @Test
    void startMustBeBeforeClose() {
        assertThrows(BadRequestException.class, () -> rfqService.create(
                new CreateRfqRequest("x", CLOSE, START, FORCED, null, 10, 5, TriggerType.BID_IN_WINDOW)));
    }

    // ---------------------------------------------------------------- ranking and rebids

    @Test
    void ranksByTotalAndRebidMustBeLower() {
        Long id = rfq(TriggerType.BID_IN_WINDOW);
        bid(id, "17:10", "A", 1000);
        bid(id, "17:11", "B", 900);
        bid(id, "17:12", "C", 950);
        assertEquals(List.of("B", "C", "A"), names(id));

        assertThrows(BadRequestException.class, () -> bid(id, "17:13", "A", 1000)); // not lower
        bid(id, "17:14", "A", 800);
        assertEquals(List.of("A", "B", "C"), names(id));
        assertEquals("L1", auctionService.rankedBidResponses(id).get(0).rank());
    }

    @Test
    void tieGoesToEarlierBid() {
        Long id = rfq(TriggerType.BID_IN_WINDOW);
        bid(id, "17:10", "A", 900);
        bid(id, "17:11", "B", 900);
        assertEquals(List.of("A", "B"), names(id));
    }

    @Test
    void bidBeforeStartIsRejected() {
        Long id = rfq(TriggerType.BID_IN_WINDOW);
        assertThrows(BadRequestException.class, () -> bid(id, "16:30", "A", 1000));
    }

    // ---------------------------------------------------------------- trigger (a) and the cap

    @Test
    void bidOutsideWindowDoesNotExtend() {
        Long id = rfq(TriggerType.BID_IN_WINDOW);
        bid(id, "17:45", "A", 1000);
        assertEquals(CLOSE, closeOf(id));
    }

    @Test
    void bidExactlyAtWindowStartExtends() {
        Long id = rfq(TriggerType.BID_IN_WINDOW);
        bid(id, "17:50", "A", 1000);
        assertEquals(t("18:05"), closeOf(id));
    }

    @Test
    void extensionsAccumulateFromCurrentClose() {
        Long id = rfq(TriggerType.BID_IN_WINDOW);
        bid(id, "17:55", "A", 1000);
        assertEquals(t("18:05"), closeOf(id));
        bid(id, "18:02", "B", 900);            // window now 17:55-18:05
        assertEquals(t("18:10"), closeOf(id));
    }

    @Test
    void extensionIsCappedAtForcedClose() {
        Long id = rfq(TriggerType.BID_IN_WINDOW, t("18:18"));
        bid(id, "18:15", "A", 1000);
        assertEquals(FORCED, closeOf(id));     // 18:20, not 18:23

        List<ActivityLogResponse> log = auctionService.activity(id);
        assertEquals(ActivityType.BID_PLACED, log.get(0).eventType());       // cause first
        assertEquals(ActivityType.AUCTION_EXTENDED, log.get(1).eventType()); // then effect
        assertTrue(log.get(1).capped());
    }

    @Test
    void noFurtherExtensionOnceAtCap() {
        Long id = rfq(TriggerType.BID_IN_WINDOW, t("18:18"));
        bid(id, "18:15", "A", 1000);           // -> 18:20 (cap)
        bid(id, "18:16", "B", 900);            // nothing left to add
        long extensions = auctionService.activity(id).stream()
                .filter(l -> l.eventType() == ActivityType.AUCTION_EXTENDED).count();
        assertEquals(1, extensions);
        assertEquals(FORCED, closeOf(id));
    }

    @Test
    void bidExactlyAtCloseIsRejected() {
        Long id = rfq(TriggerType.BID_IN_WINDOW);
        assertThrows(BadRequestException.class, () -> bid(id, "18:00", "A", 1000));
    }

    // ---------------------------------------------------------------- trigger (b)

    @Test
    void rankChangeExtends() {
        Long id = rfq(TriggerType.ANY_RANK_CHANGE);
        bid(id, "17:10", "A", 1000);
        bid(id, "17:11", "B", 1100);           // A=L1, B=L2
        bid(id, "17:58", "B", 900);            // B underbids A
        assertEquals(t("18:05"), closeOf(id));
    }

    @Test
    void newcomerAtBottomDoesNotExtend() {
        Long id = rfq(TriggerType.ANY_RANK_CHANGE);
        bid(id, "17:10", "A", 900);
        bid(id, "17:11", "B", 1000);
        bid(id, "17:58", "C", 1100);           // joins as L3, nobody moved
        assertEquals(CLOSE, closeOf(id));
    }

    // ---------------------------------------------------------------- trigger (c)

    @Test
    void newL1Extends() {
        Long id = rfq(TriggerType.L1_CHANGE);
        bid(id, "17:10", "A", 1000);
        bid(id, "17:59", "C", 900);            // C becomes L1
        assertEquals(t("18:05"), closeOf(id));
    }

    @Test
    void l1LoweringOwnPriceDoesNotExtend() {
        Long id = rfq(TriggerType.L1_CHANGE);
        bid(id, "17:10", "A", 1000);
        bid(id, "17:11", "B", 1100);
        bid(id, "17:58", "A", 950);            // still A
        assertEquals(CLOSE, closeOf(id));
    }

    // ---------------------------------------------------------------- status

    @Test
    void statusClosedWhenEndedNormally() {
        Long id = rfq(TriggerType.BID_IN_WINDOW);
        clock.set(t("18:30"));                 // even after forced close: it ended at 18:00
        assertEquals(RfqStatus.CLOSED, rfqService.get(id).status());
    }

    @Test
    void statusForceClosedWhenEndedAtCap() {
        Long id = rfq(TriggerType.BID_IN_WINDOW, t("18:18"));
        bid(id, "18:15", "A", 1000);
        clock.set(t("18:19"));
        assertEquals(RfqStatus.ACTIVE, rfqService.get(id).status());
        clock.set(t("18:20"));
        assertEquals(RfqStatus.FORCE_CLOSED, rfqService.get(id).status());
    }

    private List<String> names(Long id) {
        return auctionService.rankedBidResponses(id).stream().map(RankedBidResponse::supplierName).toList();
    }
}
