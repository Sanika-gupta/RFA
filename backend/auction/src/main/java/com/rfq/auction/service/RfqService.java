package com.rfq.auction.service;

import com.rfq.auction.dto.CreateRfqRequest;
import com.rfq.auction.dto.RfqResponse;
import com.rfq.auction.entity.Bid;
import com.rfq.auction.entity.Rfq;
import com.rfq.auction.enums.RfqStatus;
import com.rfq.auction.exception.BadRequestException;
import com.rfq.auction.exception.NotFoundException;
import com.rfq.auction.repository.RfqRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class RfqService {

    private final RfqRepository rfqRepository;
    private final AuctionService auctionService;
    private final Clock clock;

    public RfqService(RfqRepository rfqRepository, AuctionService auctionService, Clock clock) {
        this.rfqRepository = rfqRepository;
        this.auctionService = auctionService;
        this.clock = clock;
    }

    @Transactional
    public RfqResponse create(CreateRfqRequest req) {
        // Field-level checks (required, X > 0, Y > 0) already ran via @Valid.
        if (!req.startAt().isBefore(req.bidCloseAt())) {
            throw new BadRequestException("Bid start time must be before bid close time");
        }
        if (!req.forcedCloseAt().isAfter(req.bidCloseAt())) {
            throw new BadRequestException("Forced close time must be later than bid close time");
        }

        Rfq rfq = new Rfq();
        rfq.setName(req.name().trim());
        rfq.setStartAt(req.startAt());
        rfq.setBidCloseAt(req.bidCloseAt());
        rfq.setForcedCloseAt(req.forcedCloseAt());
        rfq.setPickupDate(req.pickupDate());
        rfq.setExtensionWindowMinutes(req.extensionWindowMinutes());
        rfq.setExtensionMinutes(req.extensionMinutes());
        rfq.setTriggerType(req.triggerType());
        rfq.setCreatedAt(clock.instant());

        return toResponse(rfqRepository.save(rfq));
    }

    @Transactional(readOnly = true)
    public List<RfqResponse> list() {
        return rfqRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RfqResponse get(Long id) {
        return toResponse(find(id));
    }

    Rfq find(Long id) {
        return rfqRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("RFQ " + id + " not found"));
    }

    /**
     * Status is derived from stored times, never stored itself.
     * FORCE_CLOSED means the auction ended because extensions pushed it to the cap.
     */
    public static RfqStatus statusOf(Rfq rfq, Instant now) {
        if (now.isBefore(rfq.getStartAt())) return RfqStatus.UPCOMING;
        if (now.isBefore(rfq.getBidCloseAt())) return RfqStatus.ACTIVE;
        if (rfq.getBidCloseAt().equals(rfq.getForcedCloseAt())) return RfqStatus.FORCE_CLOSED;
        return RfqStatus.CLOSED;
    }

    private RfqResponse toResponse(Rfq rfq) {
        List<Bid> ranked = auctionService.rankedBids(rfq.getId());
        Bid l1 = ranked.isEmpty() ? null : ranked.get(0);
        return new RfqResponse(
                rfq.getId(),
                rfq.getName(),
                rfq.getStartAt(),
                rfq.getBidCloseAt(),
                rfq.getForcedCloseAt(),
                rfq.getPickupDate(),
                rfq.getExtensionWindowMinutes(),
                rfq.getExtensionMinutes(),
                rfq.getTriggerType(),
                statusOf(rfq, clock.instant()),
                l1 == null ? null : l1.getTotal(),
                l1 == null ? null : l1.getSupplier().getName(),
                ranked.size(),
                rfq.getCreatedAt());
    }
}
