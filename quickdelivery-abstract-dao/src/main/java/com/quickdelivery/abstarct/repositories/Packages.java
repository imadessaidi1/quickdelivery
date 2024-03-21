package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.Package;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Packages extends CrudRepository<Package, Long> {
    @Query("SELECT a FROM Address a " +
            "WHERE st_distance_sphere(POINT(a.latitude, a.longitude), POINT(:latitude, :longitude)) <= :rayonEnMetres " +
            "AND a.packaged.status='NEW' " +
            "AND a.type = 'DEPARTURE'")
    List<Address> findAddressAroundPosition(@Param("latitude") String latitude,
                                          @Param("longitude") String longitude,
                                          @Param("rayonEnMetres") double rayonEnMetres);
    @Query("SELECT p " +
            "FROM Package p JOIN FETCH p.addresses a " +
            "WHERE a.latitude > :departureLatitude AND a.latitude < :arrivalLatitude " +
            "AND a.longitude > :departureLongitude AND a.longitude < :arrivalLongitude")

    List<Package> findAddressOnMyRoad(@Param("departureLatitude") String departureLatitude, @Param("arrivalLatitude") String arrivalLatitude,
                                      @Param("departureLongitude") String departureLongitude, @Param("arrivalLongitude") String arrivalLongitude);

    @Query("SELECT DISTINCT p FROM Package p " +
            "INNER JOIN p.addresses adDep " +
            "INNER JOIN p.addresses adArr " +
            "WHERE ST_Distance_Sphere(POINT(adDep.latitude, adDep.longitude), POINT(:startLat, :startLong)) < :startRadius " +
            "AND ST_Distance_Sphere(POINT(adArr.latitude, adArr.longitude), POINT(:endLat, :endLong)) < :endRadius " +
            "AND adDep.type = 'DEPARTURE' " +
            "AND adArr.type = 'ARRIVAL'")
    List<Package> findPackagesOnMyRoadByRadius(@Param("startLat") String startLat,
                                       @Param("startLong") String startLong,
                                       @Param("startRadius") double startRadius,
                                       @Param("endLat") String endLat,
                                       @Param("endLong") String endLong,
                                       @Param("endRadius") double endRadius);
    @Query("SELECT p " +
            "FROM Package p WHERE p.status = :status")
    List<Package> findPackagesByStatus(@Param("status") PACKAGE_STATUS status);

    @Modifying
    @Query("UPDATE Package p " +
            "SET p.status = :status WHERE p.id = :id")
    void updatePackagesStatus(@Param("status") PACKAGE_STATUS status, @Param("id") Long id);

    @Query("SELECT p " +
            "FROM Package p JOIN FETCH p.packageReservations r " +
            "WHERE r.deliveryPerson.id = :deliveryPersonID")
    List<Package> findPackagesByDeliveryPerson(@Param("deliveryPersonID") long deliveryPersonID);

    @Query("SELECT p " +
            "FROM Package p " +
            "WHERE p.reference = :reference")
    Package findPackageByReference(@Param("reference") String reference);

    @Query("SELECT COUNT(pr) > 0 FROM PackageReservation pr " +
            "JOIN pr.aPackage p " +
            "WHERE pr.deliveryPerson.id = :userId " +
            "AND pr.status = 0 " +
            "AND p.status = 'PICKEDUP'")
    boolean existsOngoingReservationsForUserWithPickedUpPackage(@Param("userId") Long userId);
}
