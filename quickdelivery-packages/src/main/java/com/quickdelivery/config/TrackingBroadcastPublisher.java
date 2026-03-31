package com.quickdelivery.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.abstarct.dto.PositionDTO;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TrackingBroadcastPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;
    private final Logger logger;
    private final StringRedisTemplate redisTemplate;
    private final boolean redisEnabled;
    private final String redisChannel;
    private final String instanceId;

    public TrackingBroadcastPublisher(ApplicationEventPublisher applicationEventPublisher,
                                      ObjectMapper objectMapper,
                                      Logger logger,
                                      @org.springframework.beans.factory.annotation.Autowired(required = false) StringRedisTemplate redisTemplate,
                                      @Value("${quickdelivery.tracking.redis.enabled:false}") boolean redisEnabled,
                                      @Value("${quickdelivery.tracking.redis.channel:quickdelivery:tracking:position}") String redisChannel,
                                      @Value("${quickdelivery.tracking.instance-id:${spring.application.name}:${random.uuid}}") String instanceId) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
        this.logger = logger;
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisEnabled;
        this.redisChannel = redisChannel;
        this.instanceId = instanceId;
    }

    public void broadcast(String from, Map<String, PositionDTO> updatedPositions) {
        if (updatedPositions == null || updatedPositions.isEmpty()) {
            return;
        }

        applicationEventPublisher.publishEvent(new TrackingPositionsBroadcastRequestedEvent(from, updatedPositions));
        if (!redisEnabled || redisTemplate == null) {
            return;
        }

        try {
            TrackingBroadcastEvent event = new TrackingBroadcastEvent(instanceId, from, updatedPositions);
            redisTemplate.convertAndSend(redisChannel, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            logger.warn("Unable to serialize tracking broadcast event", exception);
        } catch (Exception exception) {
            logger.warn("Unable to publish tracking broadcast event to Redis", exception);
        }
    }

    public String getInstanceId() {
        return instanceId;
    }
}
