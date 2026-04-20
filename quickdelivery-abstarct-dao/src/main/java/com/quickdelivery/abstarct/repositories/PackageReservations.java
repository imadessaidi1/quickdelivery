package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.PackageReservation;
import com.quickdelivery.abstarct.parameters.PACKAGE_RESERVATION_STATUS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PackageReservations extends JpaRepository<PackageReservation, Long> {
    @Query("SELECT pr " +
            "FROM PackageReservation pr " +
            "JOIN FETCH pr.aPackage p " +
            "WHERE p.id = :packageId " +
            "AND pr.deliveryPerson.id = :deliveryPersonId " +
            "AND pr.status = :status")
    Optional<PackageReservation> findReservationContext(@Param("packageId") Long packageId,
                                                        @Param("deliveryPersonId") Long deliveryPersonId,
                                                        @Param("status") PACKAGE_RESERVATION_STATUS status);

    @Query("""
            SELECT COUNT(DISTINCT pr.aPackage.id)
            FROM PackageReservation pr
            JOIN pr.aPackage p
            WHERE pr.deliveryPerson.id = :deliveryPersonId
            AND pr.status = :reservationStatus
            AND p.status IN :packageStatuses
            """)
    long countActiveReservationsByDeliveryPerson(@Param("deliveryPersonId") Long deliveryPersonId,
                                                 @Param("reservationStatus") PACKAGE_RESERVATION_STATUS reservationStatus,
                                                 @Param("packageStatuses") java.util.Collection<com.quickdelivery.abstarct.parameters.PACKAGE_STATUS> packageStatuses);

    @Query("""
            SELECT pr
            FROM PackageReservation pr
            JOIN FETCH pr.aPackage p
            WHERE p.id = :packageId
            AND pr.deliveryPerson.id = :deliveryPersonId
            AND pr.status = :status
            """)
    Optional<PackageReservation> findByPackageAndDeliveryPersonAndStatus(@Param("packageId") Long packageId,
                                                                         @Param("deliveryPersonId") Long deliveryPersonId,
                                                                         @Param("status") PACKAGE_RESERVATION_STATUS status);

    @Query("""
            SELECT DISTINCT pr.aPackage.id
            FROM PackageReservation pr
            WHERE pr.deliveryPerson.id = :deliveryPersonId
            AND pr.status IN :statuses
            """)
    List<Long> findPackageIdsByDeliveryPersonAndStatuses(@Param("deliveryPersonId") Long deliveryPersonId,
                                                         @Param("statuses") Collection<PACKAGE_RESERVATION_STATUS> statuses);
}
