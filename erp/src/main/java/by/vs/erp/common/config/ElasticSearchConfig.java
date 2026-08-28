package by.vs.erp.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
public class ElasticSearchConfig extends ElasticsearchConfiguration {

    @Override
    public ClientConfiguration clientConfiguration() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            return ClientConfiguration.builder()
                    .connectedTo("erp-elastic-es-http:9200")
                    .usingSsl(sslContext, (hostname, session) -> true)
                    .withBasicAuth("elastic", System.getenv("ELASTIC_PASSWORD"))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Не удалось инициализировать защищенный ElasticSearch клиент", e);
        }
    }
}

