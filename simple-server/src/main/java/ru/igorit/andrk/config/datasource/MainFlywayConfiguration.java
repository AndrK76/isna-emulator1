package ru.igorit.andrk.config.datasource;

import lombok.extern.log4j.Log4j2;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Log4j2
@Configuration
public class MainFlywayConfiguration {
    @Value("${spring.flyway.main.locations}")
    private String flywayPath;
    @Value("${spring.flyway.main.enabled:false}")
    private boolean flywayEnable;
    @Value("${spring.flyway.main.baseline-on-migrate:false}")
    private boolean baseLineOnMigrate;
    private final DataSource flywayDataSource;

    public MainFlywayConfiguration(
            @Qualifier("mainDatasource") DataSource flywayDataSource) {
        this.flywayDataSource = flywayDataSource;
    }

    @PostConstruct
    private void migrateFlyway() {
        if (flywayEnable) {
            try {
                Flyway.configure()
                        .dataSource(flywayDataSource)
                        .locations(flywayPath)
                        .load()
                        .migrate();
            } catch (FlywayException e){
                if (baseLineOnMigrate){
                    Flyway.configure()
                            .dataSource(flywayDataSource)
                            .locations(flywayPath)
                            .baselineOnMigrate(true)
                            .baselineVersion("00")
                            .load()
                            .migrate();
                } else {
                    throw e;
                }
            }
        }
    }
}
