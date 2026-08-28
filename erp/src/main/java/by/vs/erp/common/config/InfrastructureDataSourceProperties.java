package by.vs.erp.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.infrastructure")
public class InfrastructureDataSourceProperties {
    private final Datasource master = new Datasource();
    private final Datasource replica = new Datasource();

    public Datasource getMaster() { return master; }
    public Datasource getReplica() { return replica; }

    public static class Datasource {
        private String url;
        private String username;
        private String password;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
