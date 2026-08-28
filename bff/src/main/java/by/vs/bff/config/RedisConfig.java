package by.vs.bff.config;

import by.vs.bff.dto.TimeSlotDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, TimeSlotDto> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        Jackson2JsonRedisSerializer<TimeSlotDto> serializer = new Jackson2JsonRedisSerializer<>(TimeSlotDto.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, TimeSlotDto> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, TimeSlotDto> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}

