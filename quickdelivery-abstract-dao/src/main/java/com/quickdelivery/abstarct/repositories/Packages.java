package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.Package;
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
    @Query("SELECT p "+
            "FROM Package p JOIN FETCH p.addresses a "+
            "WHERE a.latitude BETWEEN :departureLatitude AND :arrivalLatitude "+
            "AND a.longitude BETWEEN :departureLongitude AND :arrivalLongitude")
    List<Package> findAddressOnMyRoad(@Param("departureLatitude") String departureLatitude, @Param("arrivalLatitude") String arrivalLatitude,
                                      @Param("departureLongitude") String departureLongitude, @Param("arrivalLongitude") String arrivalLongitude);
}
