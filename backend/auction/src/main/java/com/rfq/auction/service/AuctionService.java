package com.rfq.auction.service;

import com.rfq.auction.dto.ActivityLogResponse;
import com.rfq.auction.dto.BidResult;
import com.rfq.auction.dto.RankedBidResponse;
import com.rfq.auction.dto.SubmitBidRequest;
import com.rfq.auction.entity.ActivityLog;
import com.rfq.auction.entity.Bid;
import com.rfq.auction.entity.Rfq;
import com.rfq.auction.entity.Supplier;
import com.rfq.auction.enums.ActivityType;
import com.rfq.auction.enums.TriggerType;
import com.rfq.auction.exception.BadRequestException;
import com.rfq.auction.exception.NotFoundException;
import com.rfq.auction.repository.ActivityLogRepository;
import com.rfq.auction.repository.BidRepository;
import com.rfq.auction.repository.RfqRepository;
import com.rfq.auction.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** All British Auction rules live here: ranking, rebid rule, triggers, extension cap, logging. */
@Service
public class AuctionService {

    private final RfqRepository rfqRepository;
    private final SupplierRepository supplierRepository;
    private final BidRepository bidRepository;
    private final ActivityLogRepository logRepository;
    private final Clock clock;

    public AuctionService(RfqRepository rfqRepository, SupplierRepository supplierRepository,
                          BidRepository bidRepository, ActivityLogRepository logRepository, Clock clock) {
        this.rfqRepository = rfqRepository;
        this.supplierRepository = supplierRepository;
        this.bidRepository = bidRepository;
        this.logRepository = logRepository;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ submit a bid

    /**
     * One transaction: lock RFQ -> validate -> save bid -> re-rank -> maybe extend -> log.
     * If anything fails, all of it rolls back.
     */
    @Transactional
    public BidResult submitBid(Long rfqId, SubmitBidRequest req) {
        // 1. Lock the RFQ row so concurrent bids are serialised and see the latest close time.
        Rfq rfq = rfqRepository.findByIdForUpdate(rfqId)
                .orElseThrow(() -> new NotFoundException("RFQ " + rfqId + " not found"));

        // 2. Server time only. Open interval is [start, close).
        Instant now = clock.instant();
        if (now.isBefore(rfq.getStartAt())) {
            throw new BadRequestException("Auction has not started yet");
        }
        if (!now.isBefore(rfq.getBidCloseAt())) {
            throw new BadRequestException("Auction is closed");
        }

        // 3. Supplier identity (safe from duplicates because we hold the RFQ lock).
        String name = req.supplierName().trim();
        Supplier supplier = supplierRepository.findByRfqIdAndName(rfqId, name)
                .orElseGet(() -> supplierRepository.save(new Supplier(rfq, name)));

        // 4. Ranking before this bid, and this supplier's current bid in it.
        List<Bid> before = rankedBids(rfqId);
        BigDecimal total = req.freightCharge().add(req.originCharge()).add(req.destinationCharge());
        for (Bid b : before) {
            if (b.getSupplier().getId().equals(supplier.getId())
                    && total.compareTo(b.getTotal()) >= 0) {
                throw new BadRequestException(
                        "New bid (" + total + ") must be lower than your previous bid (" + b.getTotal() + ")");
            }
        }

        // 5. Save the bid (history is kept; old bids are never overwritten).
        Bid bid = bidRepository.save(new Bid(rfq, supplier,
                req.freightCharge(), req.originCharge(), req.destinationCharge(),
                req.transitTime(), req.validity(), now));

        // 6. Ranking after this bid.
        List<Bid> after = rankedBids(rfqId);

        // 7. Bid log first: the bid is the cause, the extension is the effect.
        logRepository.save(new ActivityLog(rfq, bid, ActivityType.BID_PLACED,
                name + " bid " + total, null, null, false, now));

        // 8. Does this bid extend the auction?
        boolean triggered = isTriggered(rfq.getTriggerType(), before, after);
        ExtensionResult ext = calculateExtension(rfq, now, triggered);
        if (ext.extended()) {
            rfq.setBidCloseAt(ext.newClose());   // managed entity: saved on commit
            String reason = triggerReason(rfq) + (ext.capped() ? " (capped at forced close)" : "");
            logRepository.save(new ActivityLog(rfq, bid, ActivityType.AUCTION_EXTENDED,
                    reason, ext.oldClose(), ext.newClose(), ext.capped(), now));
        }

        int rank = indexOfSupplier(after, supplier.getId()) + 1;
        return new BidResult(bid.getId(), name, total, "L" + rank, ext.extended(), rfq.getBidCloseAt());
    }

    // ------------------------------------------------------------------ rules (package-visible for tests)

    /**
     * Extend only if the bid is inside [close - X, close) and the trigger fired.
     * New close = min(close + Y, forcedClose). No extension once the close is already at the cap.
     */
    static ExtensionResult calculateExtension(Rfq rfq, Instant now, boolean triggered) {
        Instant close = rfq.getBidCloseAt();
        Instant windowStart = close.minus(rfq.getExtensionWindowMinutes(), ChronoUnit.MINUTES);

        if (!triggered || now.isBefore(windowStart) || !now.isBefore(close)) {
            return ExtensionResult.none(close);
        }

        Instant proposed = close.plus(rfq.getExtensionMinutes(), ChronoUnit.MINUTES);
        Instant newClose = proposed.isAfter(rfq.getForcedCloseAt()) ? rfq.getForcedCloseAt() : proposed;

        if (!newClose.isAfter(close)) {          // already at the cap: nothing to add
            return ExtensionResult.none(close);
        }
        boolean capped = newClose.equals(rfq.getForcedCloseAt());
        return new ExtensionResult(true, close, newClose, capped);
    }

    /** before/after are ranked lists (index 0 = L1). */
    static boolean isTriggered(TriggerType type, List<Bid> before, List<Bid> after) {
        return switch (type) {
            // (a) any accepted bid in the window counts.
            case BID_IN_WINDOW -> true;

            // (b) some supplier who was already ranked now has a different position.
            //     A newcomer joining at the bottom moves nobody, so it doesn't count.
            case ANY_RANK_CHANGE -> {
                for (int oldPos = 0; oldPos < before.size(); oldPos++) {
                    Long supplierId = before.get(oldPos).getSupplier().getId();
                    if (indexOfSupplier(after, supplierId) != oldPos) yield true;
                }
                yield false;
            }

            // (c) the L1 supplier is different. First-ever bid counts (nobody -> someone).
            case L1_CHANGE -> {
                if (after.isEmpty()) yield false;
                if (before.isEmpty()) yield true;
                yield !before.get(0).getSupplier().getId().equals(after.get(0).getSupplier().getId());
            }
        };
    }

    private static String triggerReason(Rfq rfq) {
        int x = rfq.getExtensionWindowMinutes();
        return switch (rfq.getTriggerType()) {
            case BID_IN_WINDOW -> "Bid received in last " + x + " min";
            case ANY_RANK_CHANGE -> "Supplier rank changed in last " + x + " min";
            case L1_CHANGE -> "Lowest bidder (L1) changed in last " + x + " min";
        };
    }

    private static int indexOfSupplier(List<Bid> ranked, Long supplierId) {
        for (int i = 0; i < ranked.size(); i++) {
            if (ranked.get(i).getSupplier().getId().equals(supplierId)) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ ranking and reads

    /**
     * Latest bid per supplier, sorted by total ascending.
     * Ties: earlier bid first, then lower id, so the order is always deterministic.
     */
    @Transactional(readOnly = true)
    public List<Bid> rankedBids(Long rfqId) {
        Map<Long, Bid> latestBySupplier = new HashMap<>();
        for (Bid b : bidRepository.findByRfqIdOrderByCreatedAtAscIdAsc(rfqId)) {
            latestBySupplier.put(b.getSupplier().getId(), b);   // later bids overwrite earlier ones
        }
        List<Bid> ranked = new ArrayList<>(latestBySupplier.values());
        ranked.sort(Comparator.comparing(Bid::getTotal)
                .thenComparing(Bid::getCreatedAt)
                .thenComparing(Bid::getId));
        return ranked;
    }

    @Transactional(readOnly = true)
    public List<RankedBidResponse> rankedBidResponses(Long rfqId) {
        requireRfq(rfqId);
        List<Bid> ranked = rankedBids(rfqId);
        List<RankedBidResponse> out = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            Bid b = ranked.get(i);
            out.add(new RankedBidResponse("L" + (i + 1), b.getId(), b.getSupplier().getName(),
                    b.getFreightCharge(), b.getOriginCharge(), b.getDestinationCharge(), b.getTotal(),
                    b.getTransitTime(), b.getValidity(), b.getCreatedAt()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> activity(Long rfqId) {
        requireRfq(rfqId);
        return logRepository.findByRfqIdOrderByCreatedAtAscIdAsc(rfqId).stream()
                .map(l -> new ActivityLogResponse(l.getId(), l.getEventType(), l.getReason(),
                        l.getOldClose(), l.getNewClose(), l.isCapped(),
                        l.getBid() == null ? null : l.getBid().getId(), l.getCreatedAt()))
                .toList();
    }

    private void requireRfq(Long rfqId) {
        if (!rfqRepository.existsById(rfqId)) {
            throw new NotFoundException("RFQ " + rfqId + " not found");
        }
    }
}
