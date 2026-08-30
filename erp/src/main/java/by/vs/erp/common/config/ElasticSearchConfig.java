package by.vs.erp.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
public class ElasticSearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.password}")
    private String password;

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.uris}")
    private String uris;

    @Override
    public ClientConfiguration clientConfiguration() {
        String cleanHostAndPort = uris.replace("https://", "").replace("http://", "");

        return ClientConfiguration.builder()
                .connectedTo(cleanHostAndPort)
                .usingSsl()
                .withBasicAuth(username, password)
                .build();
    }
}