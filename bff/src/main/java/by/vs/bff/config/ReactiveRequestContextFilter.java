package by.vs.bff.config;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class ReactiveRequestContextFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // записываем exchange в контекст реактивного потока
        return chain.filter(exchange)
                .contextWrite(context -> context.put(ServerWebExchange.class, exchange));
    }
}
