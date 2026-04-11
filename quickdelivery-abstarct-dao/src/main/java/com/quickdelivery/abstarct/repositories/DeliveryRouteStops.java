package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.DeliveryRouteStop;
import com.quickdelivery.abstarct.parameters.DELIVERY_ROUTE_STATUS;
import com.quickdelivery.abstarct.parameters.DELIVERY_ROUTE_STOP_KIND;
import com.quickdelivery.abstarct.parameters.DELIVERY_ROUTE_STOP_STATUS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRouteStops extends JpaRepository<DeliveryRouteStop, Long> {
    @Query("""
            SELECT drs
            FROM DeliveryRouteStop drs
            JOIN FETCH drs.deliveryRoute dr
            JOIN FETCH drs.aPackage p
            WHERE dr.deliveryPerson.id = :deliveryPersonId
            AND p.id = :packageId
            AND drs.kind = :kind
            AND dr.status IN :routeStatuses
            AND drs.status = :stopStatus
            ORDER BY dr.createdAt DESC, dr.id DESC
            """)
    Optional<DeliveryRouteStop> findFirstPendingStop(@Param("deliveryPersonId") Long deliveryPersonId,
                                                     @Param("packageId") Long packageId,
                                                     @Param("kind") DELIVERY_ROUTE_STOP_KIND kind,
                                                     @Param("routeStatuses") Collection<DELIVERY_ROUTE_STATUS> routeStatuses,
                                                     @Param("stopStatus") DELIVERY_ROUTE_STOP_STATUS stopStatus);

    @Query("""
            SELECT COUNT(drs)
            FROM DeliveryRouteStop drs
            WHERE drs.deliveryRoute.id = :routeId
            AND drs.status = :status
            """)
    long countByRouteAndStatus(@Param("routeId") Long routeId,
                               @Param("status") DELIVERY_ROUTE_STOP_STATUS status);

    @Query("""
            SELECT drs
            FROM DeliveryRouteStop drs
            JOIN FETCH drs.deliveryRoute dr
            JOIN FETCH drs.aPackage p
            WHERE dr.id = :routeId
            AND drs.status = :stopStatus
            ORDER BY drs.stopOrder ASC
            """)
    List<DeliveryRouteStop> findPendingStopsByRouteIdOrderByStopOrder(@Param("routeId") Long routeId,
                                                                      @Param("stopStatus") DELIVERY_ROUTE_STOP_STATUS stopStatus);

    @Query("""
            SELECT drs
            FROM DeliveryRouteStop drs
            JOIN FETCH drs.aPackage p
            WHERE drs.deliveryRoute.id = :routeId
            ORDER BY drs.stopOrder ASC
            """)
    List<DeliveryRouteStop> findByRouteIdOrderByStopOrder(@Param("routeId") Long routeId);
}
