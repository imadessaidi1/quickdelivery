package com.quickdelivery.services.implementations;

import com.quickdelivery.abstarct.entities.MobileDevice;
import com.quickdelivery.abstarct.entities.Notification;
import com.quickdelivery.services.interfaces.IPushNotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(prefix = "quickdelivery.push.firebase", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPushNotificationService implements IPushNotificationService {
    @Override
    public void sendNotification(Notification notification, List<MobileDevice> targetDevices) {
        // Placeholder until FCM/APNs integration is configured.
    }
}
