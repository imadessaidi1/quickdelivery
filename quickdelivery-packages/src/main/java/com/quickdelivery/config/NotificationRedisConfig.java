package com.quickdelivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import com.quickdelivery.services.implementations.NotificationRedisSubscriber;

@Configuration
public class NotificationRedisConfig {

    public static final String NOTIFICATION_CHANNEL = "quickdelivery:notifications";

    @Bean
    public RedisMessageListenerContainer notificationRedisContainer(RedisConnectionFactory connectionFactory,
                                                                    MessageListenerAdapter notificationMessageListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(notificationMessageListener, new PatternTopic(NOTIFICATION_CHANNEL));
        return container;
    }

    @Bean
    public MessageListenerAdapter notificationMessageListener(NotificationRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}
