package com.rfq.auction.controller;

import com.rfq.auction.dto.ActivityLogResponse;
import com.rfq.auction.dto.BidResult;
import com.rfq.auction.dto.RankedBidResponse;
import com.rfq.auction.dto.SubmitBidRequest;
import com.rfq.auction.service.AuctionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rfqs/{rfqId}")
public class BidController {

    private final AuctionService auctionService;

    public BidController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping("/bids")
    @ResponseStatus(HttpStatus.CREATED)
    public BidResult submit(@PathVariable Long rfqId, @Valid @RequestBody SubmitBidRequest req) {
        return auctionService.submitBid(rfqId, req);
    }

    @GetMapping("/bids")
    public List<RankedBidResponse> rankedBids(@PathVariable Long rfqId) {
        return auctionService.rankedBidResponses(rfqId);
    }

    @GetMapping("/activity")
    public List<ActivityLogResponse> activity(@PathVariable Long rfqId) {
        return auctionService.activity(rfqId);
    }
}
