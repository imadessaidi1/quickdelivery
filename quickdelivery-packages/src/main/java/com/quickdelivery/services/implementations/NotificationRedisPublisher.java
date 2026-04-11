package com.quickdelivery.services.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.config.NotificationRedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationRedisPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationRedisPublisher.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Publishes a notification for a user to Redis.
     * All instances will receive it and check if they have a local WebSocket session for this user.
     */
    public void publishNotification(String userId, String payload) {
        try {
            NotificationRedisSubscriber.RedisNotificationMessage message = 
                new NotificationRedisSubscriber.RedisNotificationMessage(userId, payload);
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(NotificationRedisConfig.NOTIFICATION_CHANNEL, jsonMessage);
            logger.debug("Published notification for user {} to Redis channel", userId);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing notification for Redis: {}", e.getMessage());
        }
    }
}
