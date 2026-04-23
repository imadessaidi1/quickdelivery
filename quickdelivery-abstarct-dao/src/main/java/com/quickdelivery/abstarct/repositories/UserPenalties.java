package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.UserPenalty;
import com.quickdelivery.abstarct.parameters.USER_PENALTY_STATUS;
import com.quickdelivery.abstarct.parameters.USER_PENALTY_TYPE;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPenalties extends JpaRepository<UserPenalty, Long> {
    @Query("""
            SELECT COUNT(up) > 0
            FROM UserPenalty up
            WHERE up.user.id = :userId
            AND up.aPackage.id = :packageId
            AND up.type = :type
            AND up.status = :status
            """)
    boolean existsActivePenaltyForPackage(@Param("userId") Long userId,
                                          @Param("packageId") Long packageId,
                                          @Param("type") USER_PENALTY_TYPE type,
                                          @Param("status") USER_PENALTY_STATUS status);
}
