package com.quickdelivery.services.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.abstarct.dto.MessageDTO;
import com.quickdelivery.abstarct.dto.MobileDeviceRegistrationDTO;
import com.quickdelivery.abstarct.dto.NotificationDTO;
import com.quickdelivery.abstarct.entities.MobileDevice;
import com.quickdelivery.abstarct.entities.Notification;
import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.parameters.NOTIFICATION_EVENT_TYPE;
import com.quickdelivery.abstarct.repositories.MobileDevices;
import com.quickdelivery.abstarct.repositories.Notifications;
import com.quickdelivery.abstarct.repositories.Users;
import com.quickdelivery.config.WebSocketHandler;
import com.quickdelivery.services.interfaces.IPushNotificationService;
import com.quickdelivery.services.interfaces.INotificationService;
import com.quickdelivery.services.implementations.NotificationRedisPublisher;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class NotificationService implements INotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private static final double EARTH_RADIUS_METERS = 6371000d;
    private static final Duration NEARBY_LOCATION_MAX_AGE = Duration.ofHours(6);

    @Autowired
    private Notifications notifications;
    @Autowired
    private Users users;
    @Autowired
    private MobileDevices mobileDevices;
    @Autowired
    private WebSocketHandler webSocketHandler;
    @Autowired
    private NotificationMessageLocalizer notificationMessageLocalizer;
    @Autowired
    private IPushNotificationService pushNotificationService;

    @Autowired
    private NotificationRedisPublisher notificationRedisPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("packageAsyncTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    @Transactional
    public NotificationDTO createAndDispatch(Long recipientUserId,
                                             NOTIFICATION_EVENT_TYPE eventType,
                                             String targetUrl,
                                             String payloadJson,
                                             Object... messageArguments) {
        User recipient = users.findById(recipientUserId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Recipient not found"));
        Locale recipientLocale = resolveRecipientLocale(recipientUserId, recipient);
        NotificationMessageLocalizer.LocalizedNotification localizedNotification =
                notificationMessageLocalizer.localize(eventType, recipientLocale, messageArguments);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setEventType(eventType);
        notification.setTitle(localizedNotification.title());
        notification.setBody(localizedNotification.body());
        notification.setTargetUrl(targetUrl);
        notification.setPayloadJson(payloadJson);
        notification.setSourceService("PACKAGE_SERVICE");
        notification.setRead(false);
        notification.setCreatedAt(Timestamp.from(Instant.now()));
        notification = notifications.save(notification);

        dispatchRealtime(notification);

        logger.info("[NOTIFICATION] Type: {}, Destinataire: {} (ID: {}), Titre: {}", 
                notification.getEventType(), recipient.getEmailAddress(), recipient.getId(), notification.getTitle());

        return toDto(notification);
    }

    @Override
    public List<NotificationDTO> findByRecipient(Long recipientUserId) {
        return notifications.findByRecipientIdOrderByCreatedAtDesc(recipientUserId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long recipientUserId) {
        Notification notification = notifications.findDetailedById(notificationId);
        if (notification == null || notification.getRecipient() == null
                || !notification.getRecipient().getId().equals(recipientUserId)) {
            throw new ResponseStatusException(NOT_FOUND, "Notification not found");
        }
        notification.setRead(true);
        notification.setReadAt(Timestamp.from(Instant.now()));
        notifications.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long recipientUserId) {
        notifications.markAllAsRead(recipientUserId, Timestamp.from(Instant.now()));
    }

    @Override
    @Transactional
    public MobileDeviceRegistrationDTO registerMobileDevice(MobileDeviceRegistrationDTO request) {
        if (request == null || request.getUserId() == null || request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Missing device registration data");
        }

        User user = users.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        MobileDevice mobileDevice = mobileDevices.findByUserIdAndDeviceId(request.getUserId(), request.getDeviceId())
                .orElseGet(MobileDevice::new);
        Timestamp now = Timestamp.from(Instant.now());
        if (mobileDevice.getCreatedAt() == null) {
            mobileDevice.setCreatedAt(now);
        }
        mobileDevice.setUser(user);
        mobileDevice.setDeviceId(request.getDeviceId());
        String pushToken = normalizePushToken(request.getPushToken());
        if (pushToken != null) {
            mobileDevice.setPushToken(pushToken);
        }
        mobileDevice.setLocale(normalizeLocaleCode(request.getLocale()));
        mobileDevice.setPlatform(request.getPlatform());
        mobileDevice.setActive(request.getActive() == null || request.getActive());
        mobileDevice.setUpdatedAt(now);
        mobileDevice.setLastSeenAt(now);
        if (request.getLatitude() != null && request.getLongitude() != null) {
            mobileDevice.setLastLatitude(request.getLatitude());
            mobileDevice.setLastLongitude(request.getLongitude());
            mobileDevice.setLastLocationAt(now);
        }
        mobileDevice = mobileDevices.save(mobileDevice);
        if (mobileDevice.getLocale() != null && !mobileDevice.getLocale().isBlank()) {
            user.setPreferredLocale(mobileDevice.getLocale());
            users.save(user);
        }

        MobileDeviceRegistrationDTO response = new MobileDeviceRegistrationDTO();
        response.setUserId(mobileDevice.getUser().getId());
        response.setDeviceId(mobileDevice.getDeviceId());
        response.setPushToken(mobileDevice.getPushToken());
        response.setLocale(mobileDevice.getLocale());
        response.setPlatform(mobileDevice.getPlatform());
        response.setActive(mobileDevice.getActive());
        response.setLatitude(mobileDevice.getLastLatitude());
        response.setLongitude(mobileDevice.getLastLongitude());
        return response;
    }

    @Override
    @Transactional
    public void unregisterMobileDevice(Long userId, String deviceId) {
        if (userId == null || deviceId == null || deviceId.isBlank()) {
            return;
        }
        mobileDevices.findByUserIdAndDeviceId(userId, deviceId).ifPresent(device -> {
            device.setActive(false);
            device.setUpdatedAt(Timestamp.from(Instant.now()));
            mobileDevices.save(device);
        });
    }

    @Override
    @Transactional
    public void updateUserPreferredLocale(Long userId, String localeCode) {
        if (userId == null) {
            return;
        }
        String normalizedLocale = normalizeLocaleCode(localeCode);
        if (normalizedLocale == null) {
            return;
        }
        users.findById(userId).ifPresent(user -> {
            user.setPreferredLocale(normalizedLocale);
            users.save(user);
        });
    }

    @Override
    public List<Long> findNearbyCourierRecipientIds(BigDecimal latitude, BigDecimal longitude, double radiusMeters) {
        if (latitude == null || longitude == null || radiusMeters <= 0d) {
            return List.of();
        }
        Timestamp since = Timestamp.from(Instant.now().minus(NEARBY_LOCATION_MAX_AGE));
        List<MobileDevice> devices = mobileDevices.findActiveDeliveryDevicesWithRecentLocation(since);
        if (devices.isEmpty()) {
            return List.of();
        }

        double targetLatitude = latitude.doubleValue();
        double targetLongitude = longitude.doubleValue();
        Set<Long> recipientIds = new LinkedHashSet<>();
        for (MobileDevice device : devices) {
            if (device.getUser() == null || device.getUser().getId() == null
                    || device.getLastLatitude() == null || device.getLastLongitude() == null) {
                continue;
            }
            double distanceMeters = haversineMeters(
                    targetLatitude,
                    targetLongitude,
                    device.getLastLatitude().doubleValue(),
                    device.getLastLongitude().doubleValue()
            );
            if (distanceMeters <= radiusMeters) {
                recipientIds.add(device.getUser().getId());
            }
        }
        return recipientIds.stream().collect(Collectors.toList());
    }

    private void dispatchRealtime(Notification notification) {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(notification.getId());
        messageDTO.setType(toFrontendType(notification.getEventType()));
        messageDTO.setFrom(notification.getSourceService());
        messageDTO.setTo(notification.getRecipient().getId().toString());
        messageDTO.setTitle(notification.getTitle());
        messageDTO.setMessage(notification.getBody());
        messageDTO.setUrl(notification.getTargetUrl());
        messageDTO.setPayloadJson(notification.getPayloadJson());
        messageDTO.setReceivedAt(notification.getCreatedAt() == null ? null : notification.getCreatedAt().toInstant().toString());
        dispatchInAppWebSocket(notification, messageDTO);
        taskExecutor.execute(() -> dispatchMobilePush(notification, notification.getRecipient().getId()));
    }

    private void dispatchInAppWebSocket(Notification notification, MessageDTO messageDTO) {
        try {
            String payload = objectMapper.writeValueAsString(messageDTO);
            String recipientUserId = notification.getRecipient().getId().toString();
            boolean broadcasted = notificationRedisPublisher.publishNotification(recipientUserId, payload);
            if (!broadcasted) {
                webSocketHandler.sendSerializedMessageToUser(recipientUserId, payload);
            }
        } catch (JsonProcessingException exception) {
            logger.error("Error serializing notification for Redis broadcast: {}", exception.getMessage());
        } catch (RuntimeException exception) {
            logger.warn("Unable to dispatch in-app notification {}: {}", notification.getId(), exception.getMessage());
        }
    }

    private void dispatchMobilePush(Notification notification, Long recipientUserId) {
        List<MobileDevice> activeDevices = new ArrayList<>(mobileDevices.findByUserIdAndActiveTrueOrderByUpdatedAtDesc(recipientUserId));
        if (activeDevices.isEmpty()) {
            logger.info("[PUSH] Aucun appareil actif trouvé pour l'utilisateur ID: {}", recipientUserId);
            return;
        }
        
        logger.info("[PUSH] Envoi vers {} appareil(s) actif(s) pour l'utilisateur ID: {}", activeDevices.size(), recipientUserId);
        List<MobileDevice> pushTargetDevices = activeDevices.stream()
                .filter(device -> normalizePushToken(device.getPushToken()) != null)
                .toList();
        if (pushTargetDevices.isEmpty()) {
            logger.info("[PUSH] Aucun token Firebase actif trouvÃ© pour l'utilisateur ID: {} sur {} appareil(s)",
                    recipientUserId, activeDevices.size());
            return;
        }

        logger.info("[PUSH] Envoi vers {} appareil(s) avec token Firebase pour l'utilisateur ID: {}",
                pushTargetDevices.size(), recipientUserId);
        try {
            pushNotificationService.sendNotification(notification, pushTargetDevices);
            logger.info("[PUSH] Succès de la requête d'envoi pour l'utilisateur ID: {}", recipientUserId);
        } catch (Exception e) {
            logger.error("[PUSH] Échec de l'envoi pour l'utilisateur ID: {}: {}", recipientUserId, e.getMessage());
        }
    }

    private Locale resolveRecipientLocale(Long recipientUserId, User recipient) {
        String preferredLocale = normalizeLocaleCode(recipient == null ? null : recipient.getPreferredLocale());
        if (preferredLocale != null) {
            return Locale.forLanguageTag(preferredLocale);
        }
        return mobileDevices.findByUserIdAndActiveTrueOrderByUpdatedAtDesc(recipientUserId).stream()
                .map(MobileDevice::getLocale)
                .map(this::normalizeLocaleCode)
                .filter(localeCode -> localeCode != null && !localeCode.isBlank())
                .findFirst()
                .map(Locale::forLanguageTag)
                .orElse(Locale.FRENCH);
    }

    private String normalizeLocaleCode(String localeCode) {
        if (localeCode == null || localeCode.isBlank()) {
            return null;
        }
        String normalized = localeCode.replace('_', '-').trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("fr")) {
            return "fr";
        }
        if (normalized.startsWith("en")) {
            return "en";
        }
        return normalized;
    }

    private String normalizePushToken(String pushToken) {
        if (pushToken == null || pushToken.isBlank()) {
            return null;
        }
        return pushToken.trim();
    }

    private String toFrontendType(NOTIFICATION_EVENT_TYPE eventType) {
        return switch (eventType) {
            case PACKAGE_NEARBY -> "NEW_PACKAGE_NOTIFICATION";
            case PACKAGE_CREATED -> "PACKAGE_CREATED_NOTIFICATION";
            case PACKAGE_RESERVED -> "PACKAGE_RESERVED_NOTIFICATION";
            case PACKAGE_RESERVATION_OTP -> "PACKAGE_RESERVATION_OTP_NOTIFICATION";
            case PACKAGE_PICKUP_STOP_ARRIVAL -> "PACKAGE_PICKUP_STOP_ARRIVAL_NOTIFICATION";
            case PACKAGE_DELIVERY_STOP_ARRIVAL -> "PACKAGE_DELIVERY_STOP_ARRIVAL_NOTIFICATION";
            case PACKAGE_PICKUP_SUCCESS -> "PACKAGE_PICKUP_NOTIFICATION";
            case PACKAGE_DELIVERY_SUCCESS -> "PACKAGE_DELIVERY_NOTIFICATION";
            case PACKAGE_PICKED_UP -> "PACKAGE_PICKUP_NOTIFICATION";
            case PACKAGE_DELIVERED -> "PACKAGE_DELIVERY_NOTIFICATION";
            case PACKAGE_PICKUP_FAILED_SENDER_ABSENT -> "PACKAGE_PICKUP_FAILED_SENDER_ABSENT_NOTIFICATION";
            case PACKAGE_DELIVERY_FAILED_RECIPIENT_ABSENT -> "PACKAGE_DELIVERY_FAILED_RECIPIENT_ABSENT_NOTIFICATION";
            case PACKAGE_RELAY_DROPOFF_REQUIRED -> "PACKAGE_RELAY_DROPOFF_REQUIRED_NOTIFICATION";
            case PACKAGE_PAYMENT_CONFIRMED -> "PACKAGE_PAYMENT_RECEIVED_NOTIFICATION";
            case DELIVERY_ROUTE_RESERVED_WARNING -> "DELIVERY_ROUTE_RESERVED_WARNING_NOTIFICATION";
            case DELIVERY_ROUTE_CANCELLED_PENALTY -> "DELIVERY_ROUTE_CANCELLED_PENALTY_NOTIFICATION";
            default -> "UNKNOWN_NOTIFICATION";
        };
    }

    private NotificationDTO toDto(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setRecipientUserId(notification.getRecipient() == null ? null : notification.getRecipient().getId());
        dto.setEventType(notification.getEventType());
        dto.setTitle(notification.getTitle());
        dto.setBody(notification.getBody());
        dto.setTargetUrl(notification.getTargetUrl());
        dto.setPayloadJson(notification.getPayloadJson());
        dto.setRead(notification.getRead());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setReadAt(notification.getReadAt());
        return dto;
    }

    private double haversineMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
        double latitudeDistance = Math.toRadians(latitude2 - latitude1);
        double longitudeDistance = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
        return 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
