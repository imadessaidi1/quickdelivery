package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.CourierPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourierPayouts extends JpaRepository<CourierPayout, Long> {
    boolean existsByaPackageId(Long packageId);

    @Query("SELECT cp FROM CourierPayout cp " +
            "LEFT JOIN FETCH cp.deliveryPerson dp " +
            "LEFT JOIN FETCH cp.aPackage p " +
            "ORDER BY cp.createdAt DESC")
    List<CourierPayout> findAllWithRelationsOrderByCreatedAtDesc();
}
