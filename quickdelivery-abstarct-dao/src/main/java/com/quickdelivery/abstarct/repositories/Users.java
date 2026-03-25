package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.entities.Vehicle;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface Users extends JpaRepository<User, Long> {
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

    @Query("SELECT u FROM User u WHERE u.activeAccount = false")
    Page<User> findUsersForValidation(Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.activeAccount = false")
    long countUsersForValidation();

    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.user.activeAccount = false")
    long countVehiclesForValidation();

    @Query("SELECT COUNT(d) FROM Document d WHERE d.user.activeAccount = false")
    long countDocumentsForValidation();

    @Query("SELECT COUNT(d) FROM Document d WHERE d.user.activeAccount = false AND d.type IN :types")
    long countDocumentsForValidationByType(@Param("types") Collection<DOCUMENT_TYPE> types);
}
