package by.vs.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableJpaRepositories(
        basePackages = "by.vs.erp",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "by.vs.erp\\..*\\.repository\\.search\\..*"
        )
)
public class AutoserviceErpApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutoserviceErpApplication.class, args);
    }
}
