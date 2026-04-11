package com.quickdelivery.services.interfaces;

import com.quickdelivery.abstarct.entities.MobileDevice;
import com.quickdelivery.abstarct.entities.Notification;

import java.util.List;

public interface IPushNotificationService {
    void sendNotification(Notification notification, List<MobileDevice> targetDevices);
}
