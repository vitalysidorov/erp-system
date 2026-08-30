package by.vs.erp;

import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;

@TestConfiguration
public class MockInfrastructureConfig {

    @Bean
    @Primary
    public ConnectionFactory mockConnectionFactory() {
        return Mockito.mock(ConnectionFactory.class);
    }

    @Bean
    @Primary
    public RedisConnectionFactory mockRedisConnectionFactory() {
        RedisConnectionFactory factory = Mockito.mock(RedisConnectionFactory.class);
        RedisConnection connection = Mockito.mock(RedisConnection.class);
        RedisKeyCommands keyCommands = Mockito.mock(RedisKeyCommands.class);

        // Обучаем моки возвращать друг друга по цепочке, чтобы spring-data-redis не падал по NPE
        Mockito.when(factory.getConnection()).thenReturn(connection);
        Mockito.when(connection.keyCommands()).thenReturn(keyCommands);

        return factory;
    }

    @Bean
    @Primary
    public CacheManager testCacheManager() {
        // Этот менеджер перехватит аннотации @CacheEvict/@Cacheable и превратит их в безопасные пустышки
        return new NoOpCacheManager();
    }
}

