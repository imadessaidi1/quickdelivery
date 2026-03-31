package com.quickdelivery.config;

import com.quickdelivery.abstarct.dto.PositionDTO;

import java.util.Map;

public record TrackingPositionsBroadcastRequestedEvent(String from, Map<String, PositionDTO> updatedPositions) {
}
