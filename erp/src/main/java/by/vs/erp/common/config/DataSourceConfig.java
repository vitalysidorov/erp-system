package by.vs.erp.common.config;

import by.vs.erp.common.database.DataSourceType;
import by.vs.erp.common.database.RoutingDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    public static class DataSourceProperties {
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

    @Bean
    @ConfigurationProperties(prefix = "spring.infrastructure.datasource.master")
    public DataSourceProperties masterProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.infrastructure.datasource.replica")
    public DataSourceProperties replicaProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariConfig hikariConfigTemplate() {
        return new HikariConfig();
    }

    @Bean
    @Primary
    public DataSource dataSource(HikariConfig hikariConfigTemplate) {
        DataSourceProperties masterProps = masterProperties();
        DataSourceProperties replicaProps = replicaProperties();

        if (masterProps.getUrl() == null || replicaProps.getUrl() == null) {
            throw new IllegalStateException("CRITICAL ERROR: URL is null. Master properties map test failed!");
        }

        HikariDataSource masterDS = buildPool(hikariConfigTemplate, masterProps, "erp-master-pool");
        HikariDataSource replicaDS = buildPool(hikariConfigTemplate, replicaProps, "erp-replica-pool");

        RoutingDataSource routingDataSource = new RoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.MASTER, masterDS);
        targetDataSources.put(DataSourceType.REPLICA, replicaDS);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDS);

        return routingDataSource;
    }

    private HikariDataSource buildPool(HikariConfig template, DataSourceProperties props, String poolName) {
        HikariConfig config = new HikariConfig();
        template.copyStateTo(config);
        config.setJdbcUrl(props.getUrl());
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());
        config.setPoolName(poolName);
        return new HikariDataSource(config);
    }
}