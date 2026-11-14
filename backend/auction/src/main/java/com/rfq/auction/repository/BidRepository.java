package com.rfq.auction.repository;

import com.rfq.auction.entity.Bid;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {

    /** All bids of an RFQ, oldest first (supplier fetched in the same query). */
    @EntityGraph(attributePaths = "supplier")
    List<Bid> findByRfqIdOrderByCreatedAtAscIdAsc(Long rfqId);
}
