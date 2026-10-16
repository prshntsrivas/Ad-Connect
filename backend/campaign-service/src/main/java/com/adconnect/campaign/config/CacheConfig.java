package com.adconnect.campaign.config;

import com.adconnect.campaign.client.UserInfo;
import com.adconnect.campaign.dto.CampaignDtos.CampaignDto;
import com.adconnect.campaign.dto.ProfileDtos.BrandProfileDto;
import com.adconnect.campaign.dto.ProfileDtos.InfluencerProfileDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

/** Redis caches with a typed JSON serializer per cache (no polymorphic type metadata stored). */
@Slf4j
@Configuration
public class CacheConfig implements CachingConfigurer {

    /** A Redis outage degrades to uncached reads instead of failing requests. */
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache get failed for {}::{}: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache put failed for {}::{}: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache evict failed for {}::{}: {}", cache.getName(), key, exception.getMessage());
            }
        };
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheCustomizer(ObjectMapper objectMapper) {
        return builder -> builder
                .withCacheConfiguration("influencers", config(objectMapper, InfluencerProfileDto.class, Duration.ofMinutes(10)))
                .withCacheConfiguration("brands", config(objectMapper, BrandProfileDto.class, Duration.ofMinutes(10)))
                .withCacheConfiguration("campaigns", config(objectMapper, CampaignDto.class, Duration.ofMinutes(5)))
                .withCacheConfiguration("users", config(objectMapper, UserInfo.class, Duration.ofMinutes(2)));
    }

    private static <T> RedisCacheConfiguration config(ObjectMapper mapper, Class<T> type, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .prefixCacheNameWith("campaign-service::")
                .serializeValuesWith(SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(mapper, type)));
    }
}
