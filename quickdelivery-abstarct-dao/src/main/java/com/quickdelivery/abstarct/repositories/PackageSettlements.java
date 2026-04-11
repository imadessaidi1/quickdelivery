package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.PackageSettlement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface PackageSettlements extends JpaRepository<PackageSettlement, Long> {
    @Query("SELECT ps FROM PackageSettlement ps JOIN FETCH ps.aPackage p ORDER BY ps.calculatedAt DESC")
    List<PackageSettlement> findAllWithPackageOrderByCalculatedAtDesc();

    @Query("""
            SELECT COUNT(ps),
                   COALESCE(SUM(ps.customerTotalPrice), 0),
                   COALESCE(SUM(ps.platformServiceFee), 0),
                   COALESCE(SUM(ps.platformCommissionAmount), 0),
                   COALESCE(AVG(ps.customerTotalPrice), 0),
                   COALESCE(AVG(ps.courierPayoutAmount), 0)
            FROM PackageSettlement ps
            """)
    List<Object[]> loadFinancialSummary();

    @Query("""
            SELECT COUNT(ps)
            FROM PackageSettlement ps
            JOIN ps.aPackage p
            WHERE p.status = 'DELIVERED'
            """)
    long countDeliveredSettlements();

    @Query("""
            SELECT ps
            FROM PackageSettlement ps
            JOIN FETCH ps.aPackage p
            ORDER BY ps.calculatedAt DESC
            """)
    List<PackageSettlement> findRecentSettlementsWithPackage(Pageable pageable);

    @Query("""
            SELECT ps.currency
            FROM PackageSettlement ps
            WHERE ps.currency IS NOT NULL
            AND ps.currency <> ''
            ORDER BY ps.calculatedAt DESC
            """)
    List<String> findRecentCurrencies(Pageable pageable);

    @Query("""
            SELECT YEAR(ps.calculatedAt),
                   MONTH(ps.calculatedAt),
                   COALESCE(SUM(ps.customerTotalPrice), 0),
                   COALESCE(SUM(ps.platformServiceFee), 0),
                   COALESCE(SUM(ps.platformCommissionAmount), 0),
                   COALESCE(SUM(ps.courierPayoutAmount), 0)
            FROM PackageSettlement ps
            WHERE ps.calculatedAt >= :fromTimestamp
            GROUP BY YEAR(ps.calculatedAt), MONTH(ps.calculatedAt)
            ORDER BY YEAR(ps.calculatedAt), MONTH(ps.calculatedAt)
            """)
    List<Object[]> loadSettlementTrendSummary(@Param("fromTimestamp") Timestamp fromTimestamp);
}
