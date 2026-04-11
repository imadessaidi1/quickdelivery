package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.CourierPayout;
import com.quickdelivery.abstarct.parameters.COURIER_PAYOUT_STATUS;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CourierPayouts extends JpaRepository<CourierPayout, Long> {
    boolean existsByaPackageId(Long packageId);

    @Query("SELECT cp FROM CourierPayout cp " +
            "LEFT JOIN FETCH cp.deliveryPerson dp " +
            "LEFT JOIN FETCH cp.aPackage p " +
            "ORDER BY cp.createdAt DESC")
    List<CourierPayout> findAllWithRelationsOrderByCreatedAtDesc();

    @Query("""
            SELECT cp.status,
                   COUNT(cp),
                   COALESCE(SUM(cp.amount), 0)
            FROM CourierPayout cp
            GROUP BY cp.status
            """)
    List<Object[]> summarizeByStatus();

    @Query("""
            SELECT dp.id,
                   dp.firstName,
                   dp.lastName,
                   dp.emailAddress,
                   COUNT(cp),
                   COALESCE(SUM(cp.amount), 0),
                   MIN(cp.createdAt)
            FROM CourierPayout cp
            LEFT JOIN cp.deliveryPerson dp
            WHERE cp.status IN :statuses
            GROUP BY dp.id, dp.firstName, dp.lastName, dp.emailAddress
            ORDER BY COALESCE(SUM(cp.amount), 0) DESC
            """)
    List<Object[]> summarizeByStatuses(@Param("statuses") Collection<COURIER_PAYOUT_STATUS> statuses, Pageable pageable);
}
