package com.quickdelivery.services.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.config.NotificationRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationRedisPublisherTest {

    @Test
    void publishNotificationReturnsFalseWithoutTouchingRedisWhenDisabled() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        NotificationRedisPublisher publisher = buildPublisher(redisTemplate, new ObjectMapper(), false);

        boolean published = publisher.publishNotification("152", "{}");

        assertFalse(published);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void publishNotificationReturnsFalseWhenRedisIsUnavailable() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        NotificationRedisPublisher publisher = buildPublisher(redisTemplate, objectMapper, true);

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"userId\":\"152\"}");
        when(redisTemplate.convertAndSend(eq(NotificationRedisConfig.NOTIFICATION_CHANNEL), anyString()))
                .thenThrow(new DataAccessResourceFailureException("Redis down"));

        boolean published = publisher.publishNotification("152", "{}");

        assertFalse(published);
    }

    @Test
    void publishNotificationReturnsTrueWhenRedisPublishSucceeds() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        NotificationRedisPublisher publisher = buildPublisher(redisTemplate, objectMapper, true);

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"userId\":\"152\"}");
        when(redisTemplate.convertAndSend(eq(NotificationRedisConfig.NOTIFICATION_CHANNEL), anyString()))
                .thenReturn(1L);

        boolean published = publisher.publishNotification("152", "{}");

        assertTrue(published);
        verify(redisTemplate).convertAndSend(eq(NotificationRedisConfig.NOTIFICATION_CHANNEL), anyString());
    }

    private NotificationRedisPublisher buildPublisher(StringRedisTemplate redisTemplate,
                                                      ObjectMapper objectMapper,
                                                      boolean redisEnabled) {
        NotificationRedisPublisher publisher = new NotificationRedisPublisher();
        ReflectionTestUtils.setField(publisher, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(publisher, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(publisher, "redisEnabled", redisEnabled);
        return publisher;
    }
}
