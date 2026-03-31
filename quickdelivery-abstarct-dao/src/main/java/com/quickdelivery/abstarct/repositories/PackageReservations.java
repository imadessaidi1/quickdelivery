package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.PackageReservation;
import com.quickdelivery.abstarct.parameters.PACKAGE_RESERVATION_STATUS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
