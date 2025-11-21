package dev.williamnogueira.ecommerce.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CacheConfigTest {

    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;

    private CacheConfig cacheConfig;

    @BeforeEach
    void setUp() {
        cacheConfig = new CacheConfig();
        cacheConfig.setRedisHost(REDIS_HOST);
        cacheConfig.setRedisPort(REDIS_PORT);
    }

    @Test
    void testLettuceConnectionFactoryShouldUseConfiguredHostAndPort() {
        // Arrange & Act
        var factory = cacheConfig.lettuceConnectionFactory();

        // Assert
        assertThat(factory).isNotNull()
                .isInstanceOf(LettuceConnectionFactory.class);

        LettuceConnectionFactory lettuce = (LettuceConnectionFactory) factory;
        lettuce.afterPropertiesSet();

        var standalone = lettuce.getStandaloneConfiguration();
        assertThat(standalone.getHostName()).isEqualTo(REDIS_HOST);
        assertThat(standalone.getPort()).isEqualTo(REDIS_PORT);
    }

    @Test
    void testCacheManagerProvidesCachesWithOneHourTtl() {
        // Arrange
        LettuceConnectionFactory connectionFactory = (LettuceConnectionFactory) cacheConfig.lettuceConnectionFactory();
        connectionFactory.afterPropertiesSet();

        // Act
        RedisCacheManager cacheManager = cacheConfig.cacheManager(connectionFactory);

        // Assert
        RedisCache cache = (RedisCache) cacheManager.getCache("testCache");
        assertThat(cache).isNotNull();

        RedisCacheConfiguration config = cache.getCacheConfiguration();
        assertThat(config).isNotNull();
        assertThat(config.getTtlFunction().getTimeToLive("dummyKey", "dummyValue"))
                .isEqualTo(Duration.ofHours(1));
    }
}
