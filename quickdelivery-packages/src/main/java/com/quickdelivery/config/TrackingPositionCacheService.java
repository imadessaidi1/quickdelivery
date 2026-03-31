package com.quickdelivery.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.abstarct.dto.PositionDTO;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TrackingPositionCacheService {
    private final ObjectMapper objectMapper;
    private final Logger logger;
    private final StringRedisTemplate redisTemplate;
    private final boolean redisEnabled;
    private final long ttlSeconds;
    private final Map<String, PositionDTO> localFallbackCache = new ConcurrentHashMap<>();

    public TrackingPositionCacheService(ObjectMapper objectMapper,
                                        Logger logger,
                                        @org.springframework.beans.factory.annotation.Autowired(required = false) StringRedisTemplate redisTemplate,
                                        @Value("${quickdelivery.tracking.redis.enabled:false}") boolean redisEnabled,
                                        @Value("${quickdelivery.tracking.position-ttl-seconds:900}") long ttlSeconds) {
        this.objectMapper = objectMapper;
        this.logger = logger;
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisEnabled;
        this.ttlSeconds = ttlSeconds;
    }

    public void store(String packageReference, PositionDTO positionDTO) {
        if (packageReference == null || packageReference.isBlank() || positionDTO == null) {
            return;
        }

        localFallbackCache.put(packageReference, copyPosition(positionDTO));
        if (!redisEnabled || redisTemplate == null) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(
                    buildKey(packageReference),
                    objectMapper.writeValueAsString(positionDTO),
                    Duration.ofSeconds(Math.max(1, ttlSeconds))
            );
        } catch (JsonProcessingException exception) {
            logger.warn("Unable to serialize tracking position cache entry for {}", packageReference, exception);
        } catch (Exception exception) {
            logger.warn("Unable to store tracking position cache entry for {}", packageReference, exception);
        }
    }

    public Optional<PositionDTO> load(String packageReference) {
        if (packageReference == null || packageReference.isBlank()) {
            return Optional.empty();
        }

        if (redisEnabled && redisTemplate != null) {
            try {
                String rawValue = redisTemplate.opsForValue().get(buildKey(packageReference));
                if (rawValue != null && !rawValue.isBlank()) {
                    PositionDTO positionDTO = objectMapper.readValue(rawValue, PositionDTO.class);
                    localFallbackCache.put(packageReference, copyPosition(positionDTO));
                    return Optional.of(positionDTO);
                }
            } catch (Exception exception) {
                logger.warn("Unable to read tracking position cache entry for {}", packageReference, exception);
            }
        }

        return Optional.ofNullable(localFallbackCache.get(packageReference)).map(this::copyPosition);
    }

    public void evict(@Nullable String packageReference) {
        if (packageReference == null || packageReference.isBlank()) {
            return;
        }

        localFallbackCache.remove(packageReference);
        if (!redisEnabled || redisTemplate == null) {
            return;
        }

        try {
            redisTemplate.delete(buildKey(packageReference));
        } catch (Exception exception) {
            logger.warn("Unable to evict tracking position cache entry for {}", packageReference, exception);
        }
    }

    private String buildKey(String packageReference) {
        return "quickdelivery:tracking:last-position:" + packageReference;
    }

    private PositionDTO copyPosition(PositionDTO source) {
        PositionDTO copy = new PositionDTO();
        copy.setLatitude(source.getLatitude());
        copy.setLongitude(source.getLongitude());
        return copy;
    }
}
