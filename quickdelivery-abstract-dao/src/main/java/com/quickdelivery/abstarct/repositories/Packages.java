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

    @Query("SELECT p " +
            "FROM Package p WHERE p.status = :status")
    List<Package> findPackagesByStatus(@Param("status") PACKAGE_STATUS status);

    @Modifying
    @Query("UPDATE Package p " +
            "SET p.status = :status WHERE p.id = :id")
    void updatePackagesStatus(@Param("status") PACKAGE_STATUS status, @Param("id") Long id);
}
