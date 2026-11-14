package com.rfq.auction.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Every bid is kept (history). Ranking uses each supplier's latest bid. */
@Entity
@Table(name = "bids",
       indexes = @Index(name = "idx_bid_rfq_supplier_time", columnList = "rfq_id, supplier_id, created_at, id"))
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal freightCharge;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originCharge;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal destinationCharge;

    /** Transit time in days. */
    @Column(nullable = false)
    private int transitTime;

    /** Quote validity in days. */
    @Column(nullable = false)
    private int validity;

    /** Server time when the bid was accepted. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Bid() { }

    public Bid(Rfq rfq, Supplier supplier, BigDecimal freightCharge, BigDecimal originCharge,
               BigDecimal destinationCharge, int transitTime, int validity, Instant createdAt) {
        this.rfq = rfq;
        this.supplier = supplier;
        this.freightCharge = freightCharge;
        this.originCharge = originCharge;
        this.destinationCharge = destinationCharge;
        this.transitTime = transitTime;
        this.validity = validity;
        this.createdAt = createdAt;
    }

    /** Derived, not stored, so it can never disagree with the three charges. */
    @Transient
    public BigDecimal getTotal() {
        return freightCharge.add(originCharge).add(destinationCharge);
    }

    public Long getId() { return id; }
    public Rfq getRfq() { return rfq; }
    public Supplier getSupplier() { return supplier; }
    public BigDecimal getFreightCharge() { return freightCharge; }
    public BigDecimal getOriginCharge() { return originCharge; }
    public BigDecimal getDestinationCharge() { return destinationCharge; }
    public int getTransitTime() { return transitTime; }
    public int getValidity() { return validity; }
    public Instant getCreatedAt() { return createdAt; }
}
