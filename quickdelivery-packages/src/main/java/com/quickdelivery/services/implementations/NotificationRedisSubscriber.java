package com.quickdelivery.services.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.config.WebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class NotificationRedisSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(NotificationRedisSubscriber.class);

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Called by MessageListenerAdapter when a message is received from Redis.
     */
    public void onMessage(String message, String channel) {
        try {
            RedisNotificationMessage redisMessage = objectMapper.readValue(message, RedisNotificationMessage.class);
            if (redisMessage != null && redisMessage.getUserId() != null) {
                logger.debug("Received notification for user {} from Redis on channel {}", redisMessage.getUserId(), channel);
                webSocketHandler.sendSerializedMessageToUser(redisMessage.getUserId(), redisMessage.getPayload());
            }
        } catch (IOException e) {
            logger.error("Error deserializing notification message from Redis: {}", e.getMessage());
        }
    }

    public static class RedisNotificationMessage {
        private String userId;
        private String payload;

        public RedisNotificationMessage() {}

        public RedisNotificationMessage(String userId, String payload) {
            this.userId = userId;
            this.payload = payload;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }
    }
}
