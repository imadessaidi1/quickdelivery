package com.quickdelivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@ConditionalOnProperty(name = "quickdelivery.tracking.redis.enabled", havingValue = "true")
public class TrackingRedisConfig {
    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
                                                        MessageListenerAdapter trackingRedisMessageListener,
                                                        @Value("${quickdelivery.tracking.redis.channel:quickdelivery:tracking:position}") String channel) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(trackingRedisMessageListener, new PatternTopic(channel));
        return container;
    }

    @Bean
    public MessageListenerAdapter trackingRedisMessageListener(TrackingRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}
