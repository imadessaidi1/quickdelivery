package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.CourierPenalty;
import com.quickdelivery.abstarct.parameters.COURIER_PENALTY_STATUS;
import com.quickdelivery.abstarct.parameters.COURIER_PENALTY_TYPE;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Repository
public interface CourierPenalties extends JpaRepository<CourierPenalty, Long> {
    List<CourierPenalty> findByDeliveryPersonIdOrderByCreatedAtDesc(Long deliveryPersonId);

    @EntityGraph(attributePaths = {"deliveryPerson", "deliveryRoute", "aPackage"})
    List<CourierPenalty> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByDeliveryRouteIdAndTypeAndStatus(Long deliveryRouteId,
                                                    COURIER_PENALTY_TYPE type,
                                                    COURIER_PENALTY_STATUS status);

    @Query("""
            SELECT cp
            FROM CourierPenalty cp
            WHERE cp.deliveryPerson.id = :deliveryPersonId
            AND cp.status = :status
            AND cp.suspensionUntil IS NOT NULL
            AND cp.suspensionUntil > :now
            ORDER BY cp.suspensionUntil DESC
            """)
    List<CourierPenalty> findActiveSuspensions(@Param("deliveryPersonId") Long deliveryPersonId,
                                               @Param("status") COURIER_PENALTY_STATUS status,
                                               @Param("now") Timestamp now);

    @Query("""
            SELECT COUNT(cp)
            FROM CourierPenalty cp
            WHERE cp.deliveryPerson.id = :deliveryPersonId
            AND cp.type = :type
            AND cp.status IN :statuses
            AND cp.createdAt >= :createdAfter
            """)
    long countRecentByDeliveryPersonAndType(@Param("deliveryPersonId") Long deliveryPersonId,
                                            @Param("type") COURIER_PENALTY_TYPE type,
                                            @Param("statuses") Collection<COURIER_PENALTY_STATUS> statuses,
                                            @Param("createdAfter") Timestamp createdAfter);

    @Query("""
            SELECT COUNT(cp),
                   COALESCE(SUM(cp.financialPenaltyAmount), 0)
            FROM CourierPenalty cp
            WHERE cp.status = :status
            """)
    List<Object[]> summarizeByStatus(@Param("status") COURIER_PENALTY_STATUS status);

    @Query("""
            SELECT COUNT(cp)
            FROM CourierPenalty cp
            WHERE cp.status = :status
            AND cp.suspensionUntil IS NOT NULL
            AND cp.suspensionUntil > :now
            """)
    long countActiveSuspensions(@Param("status") COURIER_PENALTY_STATUS status,
                                @Param("now") Timestamp now);
}
