package com.adconnect.chat.config;

import com.adconnect.chat.service.ChatFanout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisFanoutConfig {

    @Bean
    public RedisMessageListenerContainer chatFanoutContainer(RedisConnectionFactory connectionFactory,
                                                             ChatFanout fanout) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(fanout, new ChannelTopic(ChatFanout.CHANNEL));
        return container;
    }
}
