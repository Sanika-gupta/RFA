package com.rfq.auction.controller;

import com.rfq.auction.dto.CreateRfqRequest;
import com.rfq.auction.dto.RfqResponse;
import com.rfq.auction.service.RfqService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rfqs")
public class RfqController {

    private final RfqService rfqService;

    public RfqController(RfqService rfqService) {
        this.rfqService = rfqService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RfqResponse create(@Valid @RequestBody CreateRfqRequest req) {
        return rfqService.create(req);
    }

    @GetMapping
    public List<RfqResponse> list() {
        return rfqService.list();
    }

    @GetMapping("/{id}")
    public RfqResponse get(@PathVariable Long id) {
        return rfqService.get(id);
    }
}
