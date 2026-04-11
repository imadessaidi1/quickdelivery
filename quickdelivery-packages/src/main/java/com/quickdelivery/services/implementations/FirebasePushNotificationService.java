package com.quickdelivery.services.implementations;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.quickdelivery.abstarct.entities.MobileDevice;
import com.quickdelivery.abstarct.entities.Notification;
import com.quickdelivery.abstarct.repositories.MobileDevices;
import com.quickdelivery.services.interfaces.IPushNotificationService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@ConditionalOnProperty(prefix = "quickdelivery.push.firebase", name = "enabled", havingValue = "true")
public class FirebasePushNotificationService implements IPushNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(FirebasePushNotificationService.class);

    @Value("${quickdelivery.push.firebase.credentials-path:}")
    private String credentialsPath;
    @Value("${quickdelivery.push.firebase.project-id:}")
    private String projectId;

    @Autowired
    private MobileDevices mobileDevices;

    private FirebaseMessaging firebaseMessaging;

    @PostConstruct
    public void initialize() {
        try {
            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(resolveCredentials());
            if (projectId != null && !projectId.isBlank()) {
                optionsBuilder.setProjectId(projectId);
            }
            FirebaseApp firebaseApp;
            if (FirebaseApp.getApps().isEmpty()) {
                firebaseApp = FirebaseApp.initializeApp(optionsBuilder.build());
            } else {
                firebaseApp = FirebaseApp.getInstance();
            }
            firebaseMessaging = FirebaseMessaging.getInstance(firebaseApp);
            logger.info("Firebase push notifications initialized");
        } catch (Exception exception) {
            firebaseMessaging = null;
            logger.warn("Firebase push notifications are enabled but could not be initialized: {}", exception.getMessage());
        }
    }

    @Override
    @Transactional
    public void sendNotification(Notification notification, List<MobileDevice> targetDevices) {
        if (firebaseMessaging == null || notification == null || targetDevices == null || targetDevices.isEmpty()) {
            return;
        }

        for (MobileDevice targetDevice : targetDevices) {
            String token = targetDevice == null ? null : targetDevice.getPushToken();
            if (token == null || token.isBlank()) {
                continue;
            }
            
            try {
                Message message = Message.builder()
                        .setToken(token.trim())
                        .putData("notificationId", String.valueOf(notification.getId()))
                        .putData("eventType", notification.getEventType().name())
                        .putData("title", safeValue(notification.getTitle()))
                        .putData("body", safeValue(notification.getBody()))
                        .putData("targetUrl", safeValue(notification.getTargetUrl()))
                        .putData("payloadJson", safeValue(notification.getPayloadJson()))
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder()
                                        .setChannelId("quickdelivery-realtime")
                                        .setTitle(notification.getTitle())
                                        .setBody(notification.getBody())
                                        .setClickAction("OPEN_NOTIFICATION")
                                        .build())
                                .build())
                        .setApnsConfig(ApnsConfig.builder()
                                .setAps(Aps.builder()
                                        .setCategory("quickdelivery-realtime")
                                        .setContentAvailable(true)
                                        .setMutableContent(true)
                                        .setThreadId(notification.getEventType().name())
                                        .build())
                                .build())
                        .build();
                
                String response = firebaseMessaging.send(message);
                logger.debug("[PUSH] Notification {} envoyée avec succès au token ...{}", 
                        notification.getId(), token.substring(Math.max(0, token.length() - 8)));
                
            } catch (FirebaseMessagingException e) {
                MessagingErrorCode errorCode = e.getMessagingErrorCode();
                logger.warn("[PUSH] Erreur Firebase pour la notification {} (Token ...{}): {} [{}]", 
                        notification.getId(), 
                        token.substring(Math.max(0, token.length() - 8)),
                        e.getMessage(), 
                        errorCode);
                
                if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    handleInvalidToken(targetDevice);
                }
            } catch (Exception exception) {
                logger.error("[PUSH] Erreur inattendue lors de l'envoi de la notification {}: {}", 
                        notification.getId(), exception.getMessage());
            }
        }
    }

    private void handleInvalidToken(MobileDevice device) {
        try {
            logger.info("[PUSH] Désactivation de l'appareil ID: {} suite à token invalide/expiré", device.getId());
            device.setActive(false);
            device.setUpdatedAt(java.sql.Timestamp.from(java.time.Instant.now()));
            mobileDevices.save(device);
        } catch (Exception e) {
            logger.error("[PUSH] Impossible de désactiver l'appareil {}: {}", device.getId(), e.getMessage());
        }
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            Path path = Path.of(credentialsPath);
            if (Files.exists(path)) {
                try (InputStream inputStream = new FileInputStream(path.toFile())) {
                    return GoogleCredentials.fromStream(inputStream);
                }
            }
        }
        return GoogleCredentials.getApplicationDefault();
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }
}
