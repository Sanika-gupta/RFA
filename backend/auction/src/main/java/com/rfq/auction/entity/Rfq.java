package com.rfq.auction.entity;

import com.rfq.auction.enums.TriggerType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "rfqs")
public class Rfq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Instant startAt;

    /** Current close time. Moves forward on each extension, never past forcedCloseAt. */
    @Column(nullable = false)
    private Instant bidCloseAt;

    /** Hard limit. Never changes after creation. */
    @Column(nullable = false)
    private Instant forcedCloseAt;

    /** Optional pickup / service date from the RFQ form. */
    private Instant pickupDate;

    /** X: trigger window in minutes. */
    @Column(nullable = false)
    private int extensionWindowMinutes;

    /** Y: minutes added per extension. */
    @Column(nullable = false)
    private int extensionMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TriggerType triggerType;

    @Column(nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public Instant getBidCloseAt() { return bidCloseAt; }
    public void setBidCloseAt(Instant bidCloseAt) { this.bidCloseAt = bidCloseAt; }
    public Instant getForcedCloseAt() { return forcedCloseAt; }
    public void setForcedCloseAt(Instant forcedCloseAt) { this.forcedCloseAt = forcedCloseAt; }
    public Instant getPickupDate() { return pickupDate; }
    public void setPickupDate(Instant pickupDate) { this.pickupDate = pickupDate; }
    public int getExtensionWindowMinutes() { return extensionWindowMinutes; }
    public void setExtensionWindowMinutes(int m) { this.extensionWindowMinutes = m; }
    public int getExtensionMinutes() { return extensionMinutes; }
    public void setExtensionMinutes(int m) { this.extensionMinutes = m; }
    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
