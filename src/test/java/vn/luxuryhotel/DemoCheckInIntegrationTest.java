package vn.luxuryhotel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import vn.luxuryhotel.configuration.StayPolicy;
import vn.luxuryhotel.dto.Forms;
import vn.luxuryhotel.entity.*;
import vn.luxuryhotel.repository.*;
import vn.luxuryhotel.service.*;
import vn.luxuryhotel.exception.BusinessException;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DB_URL:jdbc:mariadb://127.0.0.1:3309/luxury_hotel_test}","spring.datasource.username=${DB_USERNAME:root}","spring.datasource.password=${DB_PASSWORD:}","hotel.demo.allow-early-check-in=true"})
@ActiveProfiles({"demo","integration"}) @AutoConfigureMockMvc
@ContextConfiguration(initializers=TestDatabaseGuard.class)
@EnabledIfEnvironmentVariable(named="HOTEL_IT",matches="true")
@Import(HotelIntegrationTest.TimeConfig.class)
class DemoCheckInIntegrationTest {
    @Autowired BookingService bookings;@Autowired QueryService query;@Autowired HotelRepository db;
    @Autowired PlatformTransactionManager tx;@Autowired HotelIntegrationTest.MutableClock clock;@Autowired MockMvc mvc;@Autowired StayPolicy policy;
    private void as(String email,String role){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email,"",List.of(new SimpleGrantedAuthority("ROLE_"+role))));}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void morningCheckInAndImmediateCheckoutWorkButFutureDateAndDirtyRoomRemainBlocked() throws Exception {
        var today=LocalDate.of(2030,6,15);clock.set(today.atTime(9,0));
        var room=new TransactionTemplate(tx).execute(s->{var t=new RoomType();t.setName("DemoIT-"+UUID.randomUUID());t.setDescription("Kiểm tra chế độ demo");t.setCapacity(2);t.setPrice(new BigDecimal("1200000"));db.add(t);var r=new Room();r.setNumber("D"+UUID.randomUUID().toString().substring(0,12));r.setFloor(1);r.setRoomType(t);return db.add(r);});
        var f=new Forms.Book();f.setRoomId(room.getId());f.setCheckIn(today);f.setCheckOut(today.plusDays(2));f.setGuests(2);f.setContactName("Khách demo");f.setContactEmail("customer@luxury.local");f.setContactPhone("0901234567");
        as("customer@luxury.local","CUSTOMER");var b=bookings.create(f);
        as("staff@luxury.local","STAFF");assertThat(policy.isDemoEarlyCheckIn()).isTrue();
        assertThatThrownBy(()->bookings.transition(b.getId(),"checkin","")).isInstanceOf(BusinessException.class);
        bookings.transition(b.getId(),"confirm","");bookings.transition(b.getId(),"checkin","");bookings.transition(b.getId(),"checkout","Demo trả ngay lúc 09:00");
        assertThat(query.invoiceForBooking(b.getId()).getTotal()).isEqualByComparingTo("2400000");
        assertThat(query.booking(b.getId()).getCheckedOutAt()).isEqualTo(today.atTime(9,0));
        mvc.perform(get("/staff").with(user("staff@luxury.local").roles("STAFF"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chế độ demo")));
        as("customer@luxury.local","CUSTOMER");f.setCheckIn(today.plusDays(1));f.setCheckOut(today.plusDays(2));var future=bookings.create(f);
        as("staff@luxury.local","STAFF");bookings.transition(future.getId(),"confirm","");
        assertThatThrownBy(()->bookings.transition(future.getId(),"checkin","")).isInstanceOf(BusinessException.class).hasMessageContaining("đúng ngày");
        clock.set(today.plusDays(1).atTime(9,0));assertThatThrownBy(()->bookings.transition(future.getId(),"checkin","")).isInstanceOf(BusinessException.class).hasMessageContaining("Sẵn sàng");
    }
}
