package com.quickdelivery.config;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TrackingWebSocketBroadcaster {
    private final WebSocketHandler webSocketHandler;

    public TrackingWebSocketBroadcaster(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @EventListener
    public void onTrackingPositionsBroadcastRequested(TrackingPositionsBroadcastRequestedEvent event) {
        if (event == null || event.updatedPositions() == null || event.updatedPositions().isEmpty()) {
            return;
        }
        webSocketHandler.broadcastTrackingPositions(event.from(), event.updatedPositions());
    }
}
