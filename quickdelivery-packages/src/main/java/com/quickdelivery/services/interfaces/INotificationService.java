package com.quickdelivery.services.interfaces;

import com.quickdelivery.abstarct.dto.MobileDeviceRegistrationDTO;
import com.quickdelivery.abstarct.dto.NotificationDTO;
import com.quickdelivery.abstarct.parameters.NOTIFICATION_EVENT_TYPE;

import java.math.BigDecimal;
import java.util.List;

public interface INotificationService {
    NotificationDTO createAndDispatch(Long recipientUserId,
                                      NOTIFICATION_EVENT_TYPE eventType,
                                      String targetUrl,
                                      String payloadJson,
                                      Object... messageArguments);

    List<NotificationDTO> findByRecipient(Long recipientUserId);

    void markAsRead(Long notificationId, Long recipientUserId);

    void markAllAsRead(Long recipientUserId);

    MobileDeviceRegistrationDTO registerMobileDevice(MobileDeviceRegistrationDTO request);

    void unregisterMobileDevice(Long userId, String deviceId);

    void updateUserPreferredLocale(Long userId, String localeCode);

    List<Long> findNearbyCourierRecipientIds(BigDecimal latitude, BigDecimal longitude, double radiusMeters);
}
