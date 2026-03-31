package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.UserOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserOnboardings extends JpaRepository<UserOnboarding, Long> {
    @Query("SELECT uo FROM UserOnboarding uo JOIN FETCH uo.user u WHERE u.id = :userId")
    Optional<UserOnboarding> findByUserId(@Param("userId") Long userId);

    @Query("SELECT uo FROM UserOnboarding uo JOIN FETCH uo.user u WHERE LOWER(u.emailAddress) = LOWER(:email)")
    Optional<UserOnboarding> findByUserEmail(@Param("email") String email);
}
