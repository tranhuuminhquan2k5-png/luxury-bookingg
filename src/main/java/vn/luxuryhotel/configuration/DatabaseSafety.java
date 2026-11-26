package vn.luxuryhotel.configuration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
@Configuration
public class DatabaseSafety {
    public DatabaseSafety(Environment env){
        boolean local=env.acceptsProfiles(Profiles.of("demo","integration"));
        String user=env.getProperty("spring.datasource.username","");
        String password=env.getProperty("spring.datasource.password","");
        if(!local&&(user.equalsIgnoreCase("root")||password.isBlank()))
            throw new IllegalStateException("Ngoài profile demo/integration, cần tài khoản database riêng có mật khẩu. Xem README.");
    }
}
