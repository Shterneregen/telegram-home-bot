package random.telegramhomebot.db;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
class FlywayConfig {

//    @Bean(initMethod = "migrate")
    fun flyway(dataSource: DataSource): Flyway =
        Flyway.configure()
            .baselineOnMigrate(true)
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
}
