package com.quickdelivery.services.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.config.NotificationRedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationRedisPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationRedisPublisher.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${quickdelivery.notification.redis.enabled:false}")
    private boolean redisEnabled;

    /**
     * Publishes a notification for a user to Redis.
     * All instances will receive it and check if they have a local WebSocket session for this user.
     */
    public boolean publishNotification(String userId, String payload) {
        if (!redisEnabled) {
            logger.debug("Redis notification broadcast disabled, skipping Redis publish for user {}", userId);
            return false;
        }

        try {
            NotificationRedisSubscriber.RedisNotificationMessage message = 
                new NotificationRedisSubscriber.RedisNotificationMessage(userId, payload);
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(NotificationRedisConfig.NOTIFICATION_CHANNEL, jsonMessage);
            logger.debug("Published notification for user {} to Redis channel", userId);
            return true;
        } catch (JsonProcessingException e) {
            logger.error("Error serializing notification for Redis: {}", e.getMessage());
            return false;
        } catch (DataAccessException e) {
            logger.warn("Redis notification broadcast unavailable for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
}
