package com.quickdelivery.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class TrackingRedisSubscriber {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;
    private final Logger logger;
    private final String instanceId;

    public TrackingRedisSubscriber(ApplicationEventPublisher applicationEventPublisher,
                                   ObjectMapper objectMapper,
                                   Logger logger,
                                   @Value("${quickdelivery.tracking.instance-id:${spring.application.name}:${random.uuid}}") String instanceId) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
        this.logger = logger;
        this.instanceId = instanceId;
    }

    public void onMessage(String payload) {
        try {
            TrackingBroadcastEvent event = objectMapper.readValue(payload, TrackingBroadcastEvent.class);
            if (event.updatedPositions() == null || event.updatedPositions().isEmpty()) {
                return;
            }
            if (instanceId.equals(event.sourceInstanceId())) {
                return;
            }
            applicationEventPublisher.publishEvent(
                    new TrackingPositionsBroadcastRequestedEvent(event.from(), event.updatedPositions())
            );
        } catch (Exception exception) {
            logger.warn("Unable to process tracking broadcast event from Redis", exception);
        }
    }
}
