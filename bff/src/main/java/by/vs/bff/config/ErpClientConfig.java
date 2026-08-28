package by.vs.bff.config;

import by.vs.bff.client.ErpClient;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

@Configuration
@Slf4j
public class ErpClientConfig {

    @Value("${erp.server.host}")
    private String erpHost;

    @Value("${erp.server.port}")
    private String erpPort;

    @Bean
    public ErpClient erpClient() throws SSLException {
        String erpServiceUrl = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(erpHost)
                .port(erpPort)
                .build()
                .toUriString();

        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(sslSpec -> sslSpec.sslContext(sslContext));

        WebClient webClient = WebClient.builder()
                .baseUrl(erpServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequestAndResponse())
                .filter((request, next) ->
                        ReactiveSecurityContextHolder.getContext()
                                .map(securityContext -> securityContext.getAuthentication().getCredentials().toString())
                                .map(token -> ClientRequest.from(request)
                                        .header("Authorization", "Bearer " + token)
                                        .build())
                                .defaultIfEmpty(request)
                                .flatMap(next::exchange)
                )
                .build();

        return HttpServiceProxyFactory.builderFor(
                org.springframework.web.reactive.function.client.support.WebClientAdapter.create(webClient)
        ).build().createClient(ErpClient.class);
    }

    private ExchangeFilterFunction logRequestAndResponse() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.info("Внешний запрос BFF в ERP: {} {}", request.method(), request.url());
            return Mono.just(request);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.info("Ответ от ERP получен со статусом: {}", response.statusCode());
            return Mono.just(response);
        }));
    }
}
