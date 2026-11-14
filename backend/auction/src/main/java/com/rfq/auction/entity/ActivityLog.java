package com.rfq.auction.entity;

import com.rfq.auction.enums.ActivityType;
import jakarta.persistence.*;

import java.time.Instant;

/** Append-only audit trail: bid submissions and time extensions with their reason. */
@Entity
@Table(name = "activity_logs",
       indexes = @Index(name = "idx_log_rfq_time", columnList = "rfq_id, created_at, id"))
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    /** The bid that caused this event (null for events not tied to a bid). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id")
    private Bid bid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType eventType;

    @Column(length = 255)
    private String reason;

    private Instant oldClose;
    private Instant newClose;

    @Column(nullable = false)
    private boolean capped;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ActivityLog() { }

    public ActivityLog(Rfq rfq, Bid bid, ActivityType eventType, String reason,
                       Instant oldClose, Instant newClose, boolean capped, Instant createdAt) {
        this.rfq = rfq;
        this.bid = bid;
        this.eventType = eventType;
        this.reason = reason;
        this.oldClose = oldClose;
        this.newClose = newClose;
        this.capped = capped;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Rfq getRfq() { return rfq; }
    public Bid getBid() { return bid; }
    public ActivityType getEventType() { return eventType; }
    public String getReason() { return reason; }
    public Instant getOldClose() { return oldClose; }
    public Instant getNewClose() { return newClose; }
    public boolean isCapped() { return capped; }
    public Instant getCreatedAt() { return createdAt; }
}
