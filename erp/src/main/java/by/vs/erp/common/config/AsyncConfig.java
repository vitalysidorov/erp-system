package by.vs.erp.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Method;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable throwable, Method method, Object... obj) -> {
            log.error("Критическая ошибка в асинхронном методе: {}", method.getName());
            log.error("Исключение: ", throwable);
            // можно отправить уведомление в Prometheus
        };
    }
}
