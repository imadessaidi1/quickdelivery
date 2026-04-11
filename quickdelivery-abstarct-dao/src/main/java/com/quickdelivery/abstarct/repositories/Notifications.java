package com.quickdelivery.abstarct.repositories;

import com.quickdelivery.abstarct.entities.Notification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface Notifications extends JpaRepository<Notification, Long> {
    @EntityGraph(attributePaths = {"recipient"})
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"recipient"})
    @Query("SELECT n FROM Notification n WHERE n.id = :notificationId")
    Notification findDetailedById(@Param("notificationId") Long notificationId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.recipient.id = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") Timestamp readAt);
}
