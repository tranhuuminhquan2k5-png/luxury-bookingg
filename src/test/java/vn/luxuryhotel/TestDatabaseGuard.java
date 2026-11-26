package vn.luxuryhotel;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
/** Fail before Flyway starts if a developer accidentally points integration tests at app data. */
public class TestDatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override public void initialize(ConfigurableApplicationContext context){
        String url=context.getEnvironment().getRequiredProperty("spring.datasource.url");
        if(!url.matches("jdbc:mariadb://[^/]+/luxury_hotel_test(?:\\?.*)?"))
            throw new IllegalStateException("Integration tests require the dedicated luxury_hotel_test database.");
    }
}
