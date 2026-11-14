package com.rfq.auction.entity;

import jakarta.persistence.*;

/** A supplier is identified by name within one RFQ (no login in this version). */
@Entity
@Table(name = "suppliers",
       uniqueConstraints = @UniqueConstraint(name = "uq_rfq_supplier", columnNames = {"rfq_id", "name"}))
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @Column(nullable = false, length = 150)
    private String name;

    protected Supplier() { }

    public Supplier(Rfq rfq, String name) {
        this.rfq = rfq;
        this.name = name;
    }

    public Long getId() { return id; }
    public Rfq getRfq() { return rfq; }
    public String getName() { return name; }
}
