-- Schema design (MySQL 8). The app creates these tables through JPA (ddl-auto=update);
-- this file documents the same structure for review.

CREATE TABLE rfqs (
    id                        BIGINT PRIMARY KEY AUTO_INCREMENT,
    name                      VARCHAR(150) NOT NULL,
    start_at                  DATETIME(6)  NOT NULL,  -- UTC
    bid_close_at              DATETIME(6)  NOT NULL,  -- current close; moves on extension
    forced_close_at           DATETIME(6)  NOT NULL,  -- hard cap; never changes
    pickup_date               DATETIME(6)  NULL,
    extension_window_minutes  INT          NOT NULL,  -- X
    extension_minutes         INT          NOT NULL,  -- Y
    trigger_type              VARCHAR(20)  NOT NULL,  -- BID_IN_WINDOW | ANY_RANK_CHANGE | L1_CHANGE
    created_at                DATETIME(6)  NOT NULL
);

CREATE TABLE suppliers (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    rfq_id  BIGINT       NOT NULL,
    name    VARCHAR(150) NOT NULL,
    CONSTRAINT fk_supplier_rfq FOREIGN KEY (rfq_id) REFERENCES rfqs(id),
    CONSTRAINT uq_rfq_supplier UNIQUE (rfq_id, name)
);

CREATE TABLE bids (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    rfq_id              BIGINT        NOT NULL,
    supplier_id         BIGINT        NOT NULL,
    freight_charge      DECIMAL(12,2) NOT NULL,
    origin_charge       DECIMAL(12,2) NOT NULL,
    destination_charge  DECIMAL(12,2) NOT NULL,
    transit_time        INT           NOT NULL,  -- days
    validity            INT           NOT NULL,  -- days
    created_at          DATETIME(6)   NOT NULL,
    CONSTRAINT fk_bid_rfq      FOREIGN KEY (rfq_id)      REFERENCES rfqs(id),
    CONSTRAINT fk_bid_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    INDEX idx_bid_rfq_supplier_time (rfq_id, supplier_id, created_at, id)
);

CREATE TABLE activity_logs (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    rfq_id      BIGINT       NOT NULL,
    bid_id      BIGINT       NULL,
    event_type  VARCHAR(30)  NOT NULL,  -- BID_PLACED | AUCTION_EXTENDED
    reason      VARCHAR(255) NULL,
    old_close   DATETIME(6)  NULL,
    new_close   DATETIME(6)  NULL,
    capped      BOOLEAN      NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT fk_log_rfq FOREIGN KEY (rfq_id) REFERENCES rfqs(id),
    CONSTRAINT fk_log_bid FOREIGN KEY (bid_id) REFERENCES bids(id),
    INDEX idx_log_rfq_time (rfq_id, created_at, id)
);
