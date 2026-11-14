package com.rfq.auction.repository;

import com.rfq.auction.entity.Rfq;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RfqRepository extends JpaRepository<Rfq, Long> {

    /**
     * SELECT ... FOR UPDATE. Holds a row lock until the transaction ends, so two bids
     * on the same RFQ are processed one after the other and never read a stale close time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Rfq r WHERE r.id = :id")
    Optional<Rfq> findByIdForUpdate(@Param("id") Long id);
}
