package viettel.dac.toolserviceregistry.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced configuration for Redis caching.
 * Added support for API response caching and execution plan versioning.
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisCacheConfig extends CachingConfigurerSupport {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.cache.redis.time-to-live:30m}")
    private Duration defaultTtl;

    /**
     * Creates Redis connection factory.
     *
     * @return The Redis connection factory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        return new LettuceConnectionFactory(config);
    }

    /**
     * Creates Redis template for general operations.
     *
     * @param connectionFactory The Redis connection factory
     * @return The Redis template
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    /**
     * Creates the cache manager with Redis caches.
     * Enhanced with specialized cache settings for execution plans and API responses.
     *
     * @param connectionFactory The Redis connection factory
     * @return The cache manager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring Redis cache manager with default TTL: {}", defaultTtl);

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Configure individual cache settings
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Tool graph cache - longer TTL
        cacheConfigs.put("toolGraph", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Execution plans cache - shorter TTL
        cacheConfigs.put("executionPlans", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // API response cache - even shorter TTL
        cacheConfigs.put("apiResponses", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // API metadata cache - medium TTL
        cacheConfigs.put("apiMetadata", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // API metadata mapping cache - longer TTL
        cacheConfigs.put("apiMetadataMapping", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Parameter validation results cache - short TTL
        cacheConfigs.put("parameterValidation", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Response extraction cache - medium TTL
        cacheConfigs.put("responseExtraction", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Versioned execution plan cache - longer TTL
        cacheConfigs.put("versionedPlans", defaultConfig.entryTtl(Duration.ofHours(2)));

        // Build and return cache manager
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    /**
     * Custom key generator for caching methods.
     *
     * @return The key generator
     */
    @Override
    @Bean
    public KeyGenerator keyGenerator() {
        return new VersionAwareKeyGenerator();
    }

    /**
     * Version-aware key generator for caching.
     */
    public static class VersionAwareKeyGenerator implements KeyGenerator {
        @Override
        public Object generate(Object target, Method method, Object... params) {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName())
                    .append(":")
                    .append(method.getName());

            for (Object param : params) {
                sb.append(":");
                if (param == null) {
                    sb.append("null");
                } else {
                    // Check for special version parameter
                    if (param instanceof Map && ((Map<?, ?>) param).containsKey("version")) {
                        sb.append("v").append(((Map<?, ?>) param).get("version"));
                    } else {
                        sb.append(param.toString());
                    }
                }
            }

            return sb.toString();
        }
    }
}