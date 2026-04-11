package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.Package;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface Packages extends JpaRepository<Package, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Package p WHERE p.id = :id")
    Optional<Package> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT a FROM Address a " +
            "WHERE st_distance_sphere(POINT(a.longitude, a.latitude), POINT(:longitude, :latitude)) <= :rayonEnMetres " +
            "AND a.packaged.status='NEW' " +
            "AND a.type = 'DEPARTURE'")
    List<Address> findAddressAroundPosition(@Param("latitude") String latitude,
                                          @Param("longitude") String longitude,
                                          @Param("rayonEnMetres") double rayonEnMetres);

    @Query("SELECT DISTINCT p " +
            "FROM Package p " +
            "JOIN FETCH p.addresses allAddresses " +
            "JOIN p.addresses departureAddress " +
            "WHERE p.status = 'NEW' " +
            "AND departureAddress.type = 'DEPARTURE' " +
            "AND departureAddress.latitude BETWEEN :minLat AND :maxLat " +
            "AND departureAddress.longitude BETWEEN :minLong AND :maxLong")
    List<Package> findNewPackagesAroundDepartureBoundingBox(@Param("minLat") double minLat,
                                                            @Param("maxLat") double maxLat,
                                                            @Param("minLong") double minLong,
                                                            @Param("maxLong") double maxLong);

    @Query(value = "SELECT a.package_id " +
            "FROM address a " +
            "INNER JOIN package p ON p.id = a.package_id " +
            "WHERE p.status = 'NEW' " +
            "AND a.type = 'DEPARTURE' " +
            "AND a.latitude BETWEEN :minLat AND :maxLat " +
            "AND a.longitude BETWEEN :minLong AND :maxLong " +
            "AND ST_Distance_Sphere(POINT(a.longitude, a.latitude), POINT(:centerLng, :centerLat)) <= :rayonEnMetres " +
            "ORDER BY ST_Distance_Sphere(POINT(a.longitude, a.latitude), POINT(:centerLng, :centerLat)) ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<Long> findNearbyNewPackageIds(@Param("centerLat") double centerLat,
                                       @Param("centerLng") double centerLng,
                                       @Param("rayonEnMetres") double rayonEnMetres,
                                       @Param("minLat") double minLat,
                                       @Param("maxLat") double maxLat,
                                       @Param("minLong") double minLong,
                                       @Param("maxLong") double maxLong,
                                       @Param("limit") int limit);
    @Query("SELECT p " +
            "FROM Package p JOIN FETCH p.addresses a " +
            "WHERE a.latitude > :departureLatitude AND a.latitude < :arrivalLatitude " +
            "AND a.longitude > :departureLongitude AND a.longitude < :arrivalLongitude")

    List<Package> findAddressOnMyRoad(@Param("departureLatitude") String departureLatitude, @Param("arrivalLatitude") String arrivalLatitude,
                                      @Param("departureLongitude") String departureLongitude, @Param("arrivalLongitude") String arrivalLongitude);

    @Query("SELECT DISTINCT p FROM Package p " +
            "INNER JOIN p.addresses adDep " +
            "INNER JOIN p.addresses adArr " +
            "WHERE ST_Distance_Sphere(POINT(adDep.longitude, adDep.latitude), POINT(:startLong, :startLat)) < :startRadius " +
            "AND ST_Distance_Sphere(POINT(adArr.longitude, adArr.latitude), POINT(:endLong, :endLat)) < :endRadius " +
            "AND adDep.type = 'DEPARTURE' " +
            "AND adArr.type = 'ARRIVAL'")
    List<Package> findPackagesOnMyRoadByRadius(@Param("startLat") String startLat,
                                       @Param("startLong") String startLong,
                                       @Param("startRadius") double startRadius,
                                       @Param("endLat") String endLat,
                                       @Param("endLong") String endLong,
                                       @Param("endRadius") double endRadius);

    @Query("SELECT DISTINCT p FROM Package p " +
            "LEFT JOIN FETCH p.addresses addresses " +
            "WHERE p.status = 'NEW' " +
            "AND EXISTS (" +
            "   SELECT 1 FROM Address adDep " +
            "   WHERE adDep.packaged = p " +
            "   AND adDep.type = 'DEPARTURE' " +
            "   AND adDep.latitude BETWEEN :minLat AND :maxLat " +
            "   AND adDep.longitude BETWEEN :minLong AND :maxLong" +
            ") " +
            "AND EXISTS (" +
            "   SELECT 1 FROM Address adArr " +
            "   WHERE adArr.packaged = p " +
            "   AND adArr.type = 'ARRIVAL' " +
            "   AND adArr.latitude BETWEEN :minLat AND :maxLat " +
            "   AND adArr.longitude BETWEEN :minLong AND :maxLong" +
            ")")
    List<Package> findNewPackagesInBoundingBox(@Param("minLat") double minLat,
                                               @Param("maxLat") double maxLat,
                                               @Param("minLong") double minLong,
                                               @Param("maxLong") double maxLong);
    @Query("SELECT DISTINCT p " +
            "FROM Package p " +
            "LEFT JOIN FETCH p.addresses a " +
            "WHERE p.status = :status")
    List<Package> findPackagesByStatus(@Param("status") PACKAGE_STATUS status);

    @Query("SELECT p.id " +
            "FROM Package p " +
            "WHERE p.status = :status " +
            "ORDER BY p.creationDate DESC, p.id DESC")
    Page<Long> findPackageIdsByStatus(@Param("status") PACKAGE_STATUS status, Pageable pageable);

    @Modifying
    @Query("UPDATE Package p " +
            "SET p.status = :status WHERE p.id = :id")
    void updatePackagesStatus(@Param("status") PACKAGE_STATUS status, @Param("id") Long id);

    @Query("SELECT DISTINCT p " +
            "FROM Package p " +
            "JOIN FETCH p.packageReservations r " +
            "LEFT JOIN FETCH p.addresses a " +
            "WHERE r.deliveryPerson.id = :deliveryPersonID")
    List<Package> findPackagesByDeliveryPerson(@Param("deliveryPersonID") long deliveryPersonID);

    @Query("SELECT DISTINCT p " +
            "FROM Package p " +
            "JOIN FETCH p.addresses addresses " +
            "LEFT JOIN FETCH p.packageReservations reservations " +
            "WHERE p.id = :packageId")
    Package findPackageDetailsById(@Param("packageId") long packageId);

    @Query("SELECT DISTINCT p " +
            "FROM Package p " +
            "LEFT JOIN FETCH p.addresses addresses " +
            "LEFT JOIN FETCH p.packageSettlement settlement " +
            "LEFT JOIN FETCH p.sender sender " +
            "WHERE p.id IN :packageIds")
    List<Package> findPackagesWithAddressesByIds(@Param("packageIds") List<Long> packageIds);

    @Query("SELECT DISTINCT p " +
            "FROM Package p " +
            "LEFT JOIN FETCH p.addresses a " +
            "WHERE p.sender.id = :senderID")
    List<Package> findPackagesBySender(@Param("senderID") long senderID);

    @Query("SELECT p.status, COUNT(p) " +
            "FROM Package p " +
            "GROUP BY p.status")
    List<Object[]> countPackagesGroupedByStatus();

    @Query("SELECT MONTH(p.creationDate), COUNT(p) " +
            "FROM Package p " +
            "WHERE YEAR(p.creationDate) = :year " +
            "GROUP BY MONTH(p.creationDate)")
    List<Object[]> countPackagesByMonth(@Param("year") int year);

    @Query("SELECT MONTH(p.creationDate), COALESCE(SUM(p.deliveryPrice), 0) " +
            "FROM Package p " +
            "WHERE YEAR(p.creationDate) = :year " +
            "AND p.status = 'DELIVERED' " +
            "GROUP BY MONTH(p.creationDate)")
    List<Object[]> sumDeliveredRevenueByMonth(@Param("year") int year);

    @Query("SELECT p.id " +
            "FROM Package p " +
            "WHERE p.creationDate IS NOT NULL " +
            "ORDER BY p.creationDate DESC")
    Page<Long> findRecentPackageIds(Pageable pageable);

    @Query("SELECT DISTINCT p " +
            "FROM Package p " +
            "LEFT JOIN FETCH p.addresses a " +
            "LEFT JOIN FETCH p.packageSettlement ps " +
            "LEFT JOIN FETCH p.sender s " +
            "WHERE p.id IN :ids")
    List<Package> findRecentPackagesWithDetailsByIds(@Param("ids") List<Long> ids);

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

    @Query("SELECT p " +
            "FROM Package p JOIN FETCH p.packageReservations r " +
            "WHERE r.deliveryPerson.id = :deliveryPersonID " +
            "AND p.status = 'PICKEDUP'")
    List<Package> findPackagesInDeliveryByDeliveryPerson(@Param("deliveryPersonID") long deliveryPersonID);

    @Query("SELECT p.reference " +
            "FROM Package p JOIN p.packageReservations r " +
            "WHERE r.deliveryPerson.id = :deliveryPersonID " +
            "AND p.status = 'PICKEDUP'")
    List<String> findPackageReferencesInDeliveryByDeliveryPerson(@Param("deliveryPersonID") long deliveryPersonID);

    @Query("""
            SELECT p
            FROM Package p JOIN FETCH p.packageReservations r
            WHERE r.deliveryPerson.id = :deliveryPersonID
            AND p.status IN ('PICKEDUP', 'INDELIVERY')
            ORDER BY CASE WHEN p.status = 'INDELIVERY' THEN 0 ELSE 1 END, p.reservationDate DESC, p.id DESC
            """)
    List<Package> findActiveTrackingPackagesByDeliveryPerson(@Param("deliveryPersonID") long deliveryPersonID);

    @Modifying
    @Query("""
            UPDATE Package p
            SET p.lastPositionLatitude = :latitude, p.lastPositionLongitude = :longitude
            WHERE p.reference = :packageReference
            AND p.status IN ('PICKEDUP', 'INDELIVERY')
            """)
    int updateTrackingPositionByPackageReference(@Param("packageReference") String packageReference,
                                                 @Param("latitude") java.math.BigDecimal latitude,
                                                 @Param("longitude") java.math.BigDecimal longitude);

    @Modifying
    @Query("UPDATE Package p " +
            "SET p.lastPositionLatitude = :latitude, p.lastPositionLongitude = :longitude " +
            "WHERE p.status = 'PICKEDUP' " +
            "AND EXISTS (" +
            "   SELECT 1 FROM PackageReservation pr " +
            "   WHERE pr.aPackage = p " +
            "   AND pr.deliveryPerson.id = :deliveryPersonID" +
            ")")
    int updateTrackingPositionByDeliveryPerson(@Param("deliveryPersonID") long deliveryPersonID,
                                               @Param("latitude") java.math.BigDecimal latitude,
                                               @Param("longitude") java.math.BigDecimal longitude);
}
