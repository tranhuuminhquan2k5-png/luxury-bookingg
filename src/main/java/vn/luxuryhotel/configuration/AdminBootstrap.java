package vn.luxuryhotel.configuration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.luxuryhotel.entity.*;
import vn.luxuryhotel.repository.*;
import java.util.*;

/** Optional one-time administrator, with credentials supplied only through the environment. */
@Component @Profile("!demo & !integration") @RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {
    private final Environment env;private final HotelRepository db;private final UserRepository users;private final PasswordEncoder encoder;private final HotelTime time;
    @Override @Transactional public void run(String... args){
        String email=env.getProperty("BOOTSTRAP_ADMIN_EMAIL","").trim().toLowerCase(Locale.ROOT);
        if(email.isBlank())return;
        db.lock(SystemLock.class,1L);
        if(users.countByRoleNameAndEnabledTrue("ADMIN")>0)return;
        String password=env.getProperty("BOOTSTRAP_ADMIN_PASSWORD","");
        if(!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")||email.length()>190||password.length()<12||password.length()>60||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalStateException("Bootstrap cần email hợp lệ và mật khẩu 12–60 ký tự qua biến môi trường.");
        if(users.existsByEmail(email))throw new IllegalStateException("Email bootstrap đã tồn tại; không tự thay đổi quyền tài khoản có sẵn.");
        var u=new UserAccount();u.setEmail(email);u.setPassword(encoder.encode(password));u.setFullName("Quản trị Luxury Hotel");u.setPhone("0000000000");u.setRole(db.list("from Role where name='ADMIN'",Role.class,Map.of()).getFirst());u.setCreatedAt(time.now());db.add(u);
    }
}
