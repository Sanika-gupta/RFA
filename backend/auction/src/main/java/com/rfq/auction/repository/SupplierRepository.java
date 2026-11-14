package com.rfq.auction.repository;

import com.rfq.auction.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByRfqIdAndName(Long rfqId, String name);
}
