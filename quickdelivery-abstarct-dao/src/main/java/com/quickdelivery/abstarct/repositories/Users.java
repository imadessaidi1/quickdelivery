package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.entities.Vehicle;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface Users extends JpaRepository<User, Long> {
    List<Users> findOneByFirstName(String firstName);
    @Query("SELECT u " +
            "FROM User u WHERE u.emailAddress = :email")
    User findByEmail(@Param("email") String email);

    @EntityGraph(attributePaths = {"document", "vehicles", "personalAddress", "payments"})
    @Query("SELECT u FROM User u WHERE u.emailAddress = :email")
    User findDetailedByEmail(@Param("email") String email);

    @EntityGraph(attributePaths = {"personalAddress"})
    @Query("SELECT u FROM User u WHERE u.emailAddress = :email")
    User findProfileByEmail(@Param("email") String email);

    @Override
    @EntityGraph(attributePaths = {"document", "vehicles", "personalAddress", "payments"})
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = {"document", "vehicles", "personalAddress", "payments"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"personalAddress"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findProfileById(@Param("id") Long id);

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

    @EntityGraph(attributePaths = {"document", "vehicles", "personalAddress", "payments"})
    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    List<User> findUsersForValidationByIds(@Param("ids") Collection<Long> ids);

    @Query("SELECT u.id FROM User u WHERE u.activeAccount = false ORDER BY u.id DESC")
    Page<Long> findUserIdsForValidation(Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.activeAccount = false")
    long countUsersForValidation();

    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.user.activeAccount = false")
    long countVehiclesForValidation();

    @Query("SELECT COUNT(d) FROM Document d WHERE d.user.activeAccount = false")
    long countDocumentsForValidation();

    @Query("SELECT COUNT(d) FROM Document d WHERE d.user.activeAccount = false AND d.type IN :types")
    long countDocumentsForValidationByType(@Param("types") Collection<DOCUMENT_TYPE> types);

    long countByTypeIgnoreCase(String type);

    @Query("SELECT COUNT(u) FROM User u WHERE LOWER(u.type) = LOWER(:type)")
    long countRegisteredUsersByType(@Param("type") String type);

    @Query("SELECT COUNT(u) FROM User u WHERE LOWER(u.type) = LOWER(:type) AND u.activeAccount = true AND LOWER(u.emailAddress) IN :emails")
    long countConnectedUsersByType(@Param("type") String type, @Param("emails") Set<String> emails);
}
