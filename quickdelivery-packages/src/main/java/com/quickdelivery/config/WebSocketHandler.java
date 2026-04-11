package com.quickdelivery.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.abstarct.dto.MessageDTO;
import com.quickdelivery.abstarct.dto.PositionDTO;
import com.quickdelivery.services.interfaces.IPackagesService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private Logger logger;

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final Map<String, WebSocketSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> packageSubscribers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    @Autowired
    @Lazy
    private IPackagesService packagesService;
    @Autowired
    private TrackingPositionCacheService trackingPositionCacheService;
    @Autowired
    @Qualifier("trackingAsyncTaskExecutor")
    private Executor trackingAsyncTaskExecutor;
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        sessionsById.put(session.getId(), session);
        logger.info("WebSocket connection established: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        unregisterNotificationSession(session.getId(), session);
        sessions.remove(session);
        sessionsById.remove(session.getId());
        unregisterSession(session.getId());
        super.afterConnectionClosed(session, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.info("Received message from client " + session.getId() + ": " + message.getPayload());
        ObjectMapper objectMapper = new ObjectMapper();
        MessageDTO messageDTO = null;
        try {
            messageDTO = objectMapper.readValue(message.getPayload(), MessageDTO.class);
            if ("TRACK_PACKAGE_SUBSCRIBE".equals(messageDTO.getType())) {
                handleTrackingSubscription(session, messageDTO);
                return;
            }
            if ("REGISTER_NOTIFICATION_SESSION".equals(messageDTO.getType())) {
                registerNotificationSession(session, messageDTO.getFrom());
                return;
            }
            if ("TRACK_PACKAGE_UNSUBSCRIBE".equals(messageDTO.getType())) {
                removeSubscription(session.getId(), messageDTO.getPackageReference());
                return;
            }
            if ("PACKAGE_POSITION_UPDATE".equals(messageDTO.getType())) {
                Map<String, PositionDTO> map = packagesService.handleWebsocketMessage(messageDTO);
                broadcastTrackingUpdates(objectMapper, messageDTO, map);
            }
            if ("PACKAGE_SOFT_LOCK".equals(messageDTO.getType())) {
                handlePackageSoftLock(session, messageDTO);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleTrackingSubscription(WebSocketSession session, MessageDTO messageDTO) throws IOException {
        if (messageDTO.getPackageReference() == null || messageDTO.getPackageReference().isBlank()) {
            return;
        }

        packageSubscribers.computeIfAbsent(messageDTO.getPackageReference(), ignored -> ConcurrentHashMap.newKeySet())
                .add(session.getId());
        sessionSubscriptions.computeIfAbsent(session.getId(), ignored -> ConcurrentHashMap.newKeySet())
                .add(messageDTO.getPackageReference());

        try {
            var packageDTO = packagesService.findTrackingSubscriptionPackage(
                    messageDTO.getPackageReference(),
                    messageDTO.getGuestAccessToken()
            );

            sendTrackingSubscriptionAck(session, messageDTO.getPackageReference());

            PositionDTO positionDTO = trackingPositionCacheService.load(messageDTO.getPackageReference())
                    .orElseGet(() -> {
                        if (packageDTO.getLastPositionLatitude() == null || packageDTO.getLastPositionLongitude() == null) {
                            return null;
                        }
                        PositionDTO latestPosition = new PositionDTO();
                        latestPosition.setLatitude(packageDTO.getLastPositionLatitude());
                        latestPosition.setLongitude(packageDTO.getLastPositionLongitude());
                        return latestPosition;
                    });
            if (positionDTO != null) {
                sendTrackingUpdate(session, messageDTO.getPackageReference(), positionDTO, "PACKAGE_SERVICE");
            }
        } catch (RuntimeException exception) {
            removeSubscription(session.getId(), messageDTO.getPackageReference());
            throw exception;
        }
    }

    private void broadcastTrackingUpdates(ObjectMapper objectMapper, MessageDTO messageDTO, Map<String, PositionDTO> updatedPositions) {
        updatedPositions.forEach((packageReference, positionDTO) -> {
            Set<String> subscriberIds = packageSubscribers.getOrDefault(packageReference, Collections.emptySet());
            if (subscriberIds.isEmpty()) {
                return;
            }

            Set<String> deliveredTo = new HashSet<>();
            subscriberIds.forEach(sessionId -> {
                WebSocketSession targetSession = sessionsById.get(sessionId);
                if (targetSession == null || !targetSession.isOpen() || !deliveredTo.add(sessionId)) {
                    return;
                }
                submitTrackingSend(() -> sendTrackingUpdate(targetSession, packageReference, positionDTO, messageDTO.getFrom()), packageReference, sessionId);
            });
        });
    }

    public void broadcastTrackingPositions(String from, Map<String, PositionDTO> updatedPositions) {
        if (updatedPositions == null || updatedPositions.isEmpty()) {
            return;
        }

        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setFrom(from);
        broadcastTrackingUpdates(new ObjectMapper(), messageDTO, updatedPositions);
    }

    private void sendTrackingUpdate(WebSocketSession session, String packageReference, PositionDTO positionDTO, String from) {
        if (session == null || !session.isOpen()) {
            return;
        }

        MessageDTO response = new MessageDTO();
        response.setType("PACKAGE_POSITION_UPDATE");
        response.setFrom(from);
        response.setTo(session.getId());
        response.setPackageReference(packageReference);
        response.setPositionDTO(positionDTO);
        response.setMessage(positionDTO == null ? "" : positionDTO.getLatitude() + "," + positionDTO.getLongitude());
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            logger.warn("Unable to send tracking update for {} to session {}", packageReference, session.getId(), e);
        }
    }

    private void sendTrackingSubscriptionAck(WebSocketSession session, String packageReference) {
        if (session == null || !session.isOpen()) {
            return;
        }

        MessageDTO response = new MessageDTO();
        response.setType("TRACK_PACKAGE_SUBSCRIBED");
        response.setFrom("PACKAGE_SERVICE");
        response.setTo(session.getId());
        response.setPackageReference(packageReference);
        response.setMessage("subscribed");
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            logger.warn("Unable to send tracking subscription ack for {} to session {}", packageReference, session.getId(), e);
        }
    }

    private void submitTrackingSend(Runnable task, String packageReference, String sessionId) {
        try {
            CompletableFuture.runAsync(task, trackingAsyncTaskExecutor);
        } catch (RejectedExecutionException exception) {
            logger.warn("Dropping tracking websocket push for {} to session {} because the tracking executor is saturated", packageReference, sessionId);
        }
    }

    private void unregisterSession(String sessionId) {
        Set<String> subscribedPackages = sessionSubscriptions.remove(sessionId);
        if (subscribedPackages == null) {
            return;
        }

        subscribedPackages.forEach(packageReference -> removeSubscription(sessionId, packageReference));
    }

    private void removeSubscription(String sessionId, String packageReference) {
        if (packageReference == null || packageReference.isBlank()) {
            return;
        }

        Set<String> subscribers = packageSubscribers.get(packageReference);
        if (subscribers != null) {
            subscribers.remove(sessionId);
            if (subscribers.isEmpty()) {
                packageSubscribers.remove(packageReference);
            }
        }

        Set<String> subscribedPackages = sessionSubscriptions.get(sessionId);
        if (subscribedPackages != null) {
            subscribedPackages.remove(packageReference);
            if (subscribedPackages.isEmpty()) {
                sessionSubscriptions.remove(sessionId);
            }
        }
    }

    public void sendMessageToAll(String message) {
        for (WebSocketSession session : sessions) {
            if(session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                logger.info("Couldn't send notification, session closed");
            }
        }
    }

    public void sendMessageToUser(String userId, String message) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        Set<String> sessionIds = userSessions.getOrDefault(userId, Collections.emptySet());
        for (String sessionId : sessionIds) {
            WebSocketSession session = sessionsById.get(sessionId);
            if (session == null || !session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                logger.warn("Couldn't send notification to user {} session {}", userId, sessionId, e);
            }
        }
    }

    /**
     * Sends a pre-serialized message to a user. 
     * Useful when the message already comes in JSON format (e.g. from Redis).
     */
    public void sendSerializedMessageToUser(String userId, String serializedMessage) {
        // In this implementation, it's identical to sendMessageToUser because 
        // sendMessageToUser already takes a String 'message'.
        // We keep both for semantic clarity in NotificationService vs RedisSubscriber.
        sendMessageToUser(userId, serializedMessage);
    }

    private void registerNotificationSession(WebSocketSession session, String userId) {
        if (session == null || userId == null || userId.isBlank()) {
            return;
        }
        unregisterNotificationSession(session.getId(), session);
        userSessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session.getId());
        session.getAttributes().put("notificationUserId", userId);
    }

    private void unregisterNotificationSession(String sessionId, WebSocketSession session) {
        Object userId = session == null ? null : session.getAttributes().get("notificationUserId");
        if (!(userId instanceof String registeredUserId) || registeredUserId.isBlank()) {
            return;
        }
        Set<String> sessionIds = userSessions.get(registeredUserId);
        if (sessionIds != null) {
            sessionIds.remove(sessionId);
            if (sessionIds.isEmpty()) {
                userSessions.remove(registeredUserId);
            }
        }
    }
    private void handlePackageSoftLock(WebSocketSession session, MessageDTO messageDTO) {
        if (messageDTO.getPackageId() == null) return;
        packagesService.applySoftLock(messageDTO.getPackageId(), messageDTO.getFrom());
        
        // Broadcast to everyone else
        MessageDTO broadcast = new MessageDTO();
        broadcast.setType("PACKAGE_SOFT_LOCKED");
        broadcast.setPackageId(messageDTO.getPackageId());
        broadcast.setFrom(messageDTO.getFrom());
        
        try {
            String json = new ObjectMapper().writeValueAsString(broadcast);
            for (WebSocketSession s : sessions) {
                if (s.isOpen() && s.getId() != null && !s.getId().equals(session.getId())) {
                    try {
                        s.sendMessage(new TextMessage(json));
                    } catch (Exception e) {
                        // Ignore session failures
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Soft lock broadcast failed", e);
        }
    }
}
