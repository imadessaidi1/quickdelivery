package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.DeliveryRoute;
import com.quickdelivery.abstarct.parameters.DELIVERY_ROUTE_STATUS;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.sql.Timestamp;

@Repository
public interface DeliveryRoutes extends JpaRepository<DeliveryRoute, Long> {
    @EntityGraph(attributePaths = {"stops", "stops.aPackage", "deliveryPerson"})
    @Query("""
            SELECT dr
            FROM DeliveryRoute dr
            WHERE dr.deliveryPerson.id = :deliveryPersonId
            AND dr.status IN :statuses
            ORDER BY dr.createdAt DESC, dr.id DESC
            """)
    List<DeliveryRoute> findByDeliveryPersonAndStatuses(@Param("deliveryPersonId") Long deliveryPersonId,
                                                        @Param("statuses") Collection<DELIVERY_ROUTE_STATUS> statuses);

    @EntityGraph(attributePaths = {"stops", "stops.aPackage", "deliveryPerson"})
    @Query("""
            SELECT dr
            FROM DeliveryRoute dr
            WHERE dr.deliveryPerson.id = :deliveryPersonId
            AND dr.status IN :statuses
            ORDER BY dr.createdAt DESC, dr.id DESC
            """)
    Optional<DeliveryRoute> findFirstByDeliveryPersonAndStatuses(@Param("deliveryPersonId") Long deliveryPersonId,
                                                                 @Param("statuses") Collection<DELIVERY_ROUTE_STATUS> statuses);

    @Query("""
            SELECT COUNT(dr) > 0
            FROM DeliveryRoute dr
            WHERE dr.deliveryPerson.id = :deliveryPersonId
            AND dr.status IN :statuses
            """)
    boolean existsByDeliveryPersonAndStatuses(@Param("deliveryPersonId") Long deliveryPersonId,
                                              @Param("statuses") Collection<DELIVERY_ROUTE_STATUS> statuses);

    @EntityGraph(attributePaths = {"deliveryPerson"})
    @Query("""
            SELECT dr
            FROM DeliveryRoute dr
            WHERE dr.status = :status
            AND dr.createdAt < :createdBefore
            """)
    List<DeliveryRoute> findByStatusAndCreatedBefore(@Param("status") DELIVERY_ROUTE_STATUS status,
                                                     @Param("createdBefore") Timestamp createdBefore);

    @EntityGraph(attributePaths = {"deliveryPerson"})
    @Query("""
            SELECT dr
            FROM DeliveryRoute dr
            WHERE dr.status = :status
            AND dr.startedAt < :startedBefore
            AND NOT EXISTS (
                SELECT 1
                FROM DeliveryRouteStop drs
                WHERE drs.deliveryRoute = dr
                AND drs.status = :completedStopStatus
            )
            """)
    List<DeliveryRoute> findActiveRoutesWithoutCompletedStop(@Param("status") DELIVERY_ROUTE_STATUS status,
                                                             @Param("startedBefore") Timestamp startedBefore,
                                                             @Param("completedStopStatus") com.quickdelivery.abstarct.parameters.DELIVERY_ROUTE_STOP_STATUS completedStopStatus);
}
