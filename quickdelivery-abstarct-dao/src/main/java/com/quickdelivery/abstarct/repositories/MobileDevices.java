package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.MobileDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface MobileDevices extends JpaRepository<MobileDevice, Long> {
    Optional<MobileDevice> findByUserIdAndDeviceId(Long userId, String deviceId);
    List<MobileDevice> findByUserIdAndActiveTrueOrderByUpdatedAtDesc(Long userId);

    @Query("SELECT md FROM MobileDevice md " +
            "JOIN FETCH md.user u " +
            "WHERE md.active = true " +
            "AND md.lastLocationAt >= :since " +
            "AND md.lastLatitude IS NOT NULL " +
            "AND md.lastLongitude IS NOT NULL " +
            "AND u.type = 'DELIVERY_PERSON'")
    List<MobileDevice> findActiveDeliveryDevicesWithRecentLocation(@Param("since") Timestamp since);
}
