package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Users extends CrudRepository<User, Long> {
    List<Users> findOneByFirstName(String firstName);
    @Query("SELECT u " +
            "FROM User u WHERE u.emailAddress = :email")
    User findByEmail(@Param("email") String email);
    @Query("SELECT a FROM Address a " +
            "WHERE st_distance_sphere(POINT(a.latitude, a.longitude), POINT(:latitude, :longitude)) <= :rayonEnMetres " +
            "AND a.residents.type='DELIVERY_PERSON' " +
            "AND a.type = 'RESIDENCE'")
    List<Address> findUsersAroundPosition(@Param("latitude") String latitude,
                                          @Param("longitude") String longitude,
                                          @Param("rayonEnMetres") double rayonEnMetres);
    @Query("SELECT u " +
            "FROM User u WHERE u.activeAccount = false")
    List<User> findUsersForValidation();
}
