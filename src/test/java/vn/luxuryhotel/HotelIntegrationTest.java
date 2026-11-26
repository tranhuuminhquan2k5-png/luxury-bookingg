package vn.luxuryhotel;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import vn.luxuryhotel.service.*;
import vn.luxuryhotel.repository.*;
import vn.luxuryhotel.entity.*;
import vn.luxuryhotel.dto.Forms;
import vn.luxuryhotel.exception.BusinessException;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;

/** Development integration checks against a dedicated real MariaDB. No reset/delete commands. */
@SpringBootTest(properties={
 "spring.datasource.url=${TEST_DB_URL:jdbc:mariadb://127.0.0.1:3309/luxury_hotel_test}",
 "spring.datasource.username=${DB_USERNAME:root}","spring.datasource.password=${DB_PASSWORD:}",
 "spring.thymeleaf.cache=false","hotel.demo.allow-early-check-in=false"})
@org.springframework.test.context.ContextConfiguration(initializers=TestDatabaseGuard.class)
@AutoConfigureMockMvc @ActiveProfiles({"demo","integration"})
@EnabledIfEnvironmentVariable(named="HOTEL_IT",matches="true")
@Import(HotelIntegrationTest.TimeConfig.class)
class HotelIntegrationTest {
    @Autowired BookingService bookings;@Autowired QueryService query;@Autowired CatalogService catalog;
    @Autowired AccountService accounts;@Autowired HotelRepository db;@Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactionManager;@Autowired MockMvc mvc;@Autowired MutableClock clock;
    private final LocalDate day=LocalDate.of(2030,6,15);
    @BeforeEach void resetClock(){clock.set(day.atTime(10,0));SecurityContextHolder.clearContext();}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    <T>T tx(Supplier<T> work){return new TransactionTemplate(transactionManager).execute(s->work.get());}
    void as(String email){String role=email.startsWith("admin")?"ADMIN":email.startsWith("staff")?"STAFF":"CUSTOMER";SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email,"",List.of(new SimpleGrantedAuthority("ROLE_"+role))));}
    Room freshRoom(){return tx(()->{var t=new RoomType();t.setName("IT-"+UUID.randomUUID());t.setDescription("Phòng kiểm tra kỹ thuật");t.setCapacity(2);t.setPrice(new BigDecimal("1234567.50"));db.add(t);var r=new Room();r.setNumber("T"+UUID.randomUUID().toString().substring(0,12));r.setFloor(1);r.setRoomType(t);return db.add(r);});}
    Forms.Book request(Room room,LocalDate in,LocalDate out){var f=new Forms.Book();f.setRoomId(room.getId());f.setCheckIn(in);f.setCheckOut(out);f.setGuests(2);f.setContactName("Khách kiểm tra");f.setContactPhone("0901234567");f.setContactEmail("customer@luxury.local");return f;}
    Booking create(Room r){as("customer@luxury.local");return bookings.create(request(r,day,day.plusDays(1)));}
    HotelService service(){return tx(()->{var s=new HotelService();s.setName("IT-Service-"+UUID.randomUUID());s.setUnit("lượt");s.setPrice(new BigDecimal("100000.25"));return db.add(s);});}
    void checkIn(Booking b){as("staff@luxury.local");bookings.transition(b.getId(),"confirm","");clock.set(day.atTime(14,0));bookings.transition(b.getId(),"checkin","");}

    @Test void fullStayKeepsPricesInvoicesAndSingleSuccessfulPayment() throws Exception {
        var room=freshRoom();var b=create(room);checkIn(b);var s=service();
        var f=new Forms.UseService();f.setServiceId(s.getId());f.setQuantity(3);bookings.addService(b.getId(),f);
        tx(()->{db.get(RoomType.class,room.getRoomType().getId()).setPrice(new BigDecimal("9999999"));db.get(HotelService.class,s.getId()).setPrice(new BigDecimal("888888"));return null;});
        assertThatThrownBy(()->bookings.transition(b.getId(),"checkin","")).isInstanceOf(BusinessException.class);
        clock.set(day.plusDays(1).atTime(12,30));bookings.transition(b.getId(),"checkout","");
        var invoice=query.invoiceForBooking(b.getId());assertThat(invoice.getTotal()).isEqualByComparingTo("1534568.25");
        assertThat(query.items(invoice)).hasSize(2);assertThat(query.get(Room.class,room.getId()).getStatus()).isEqualTo(RoomStatus.DIRTY);
        assertThatThrownBy(()->bookings.addService(b.getId(),f)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->bookings.transition(b.getId(),"checkout","")).isInstanceOf(BusinessException.class);
        bookings.pay(b.getId(),false);assertThat(query.invoiceForBooking(b.getId()).isPaid()).isFalse();
        as("admin@luxury.local");var before=(BigDecimal)query.revenue(day,day.plusDays(1),true,0).get("total");
        as("staff@luxury.local");bookings.pay(b.getId(),true);
        assertThatThrownBy(()->bookings.pay(b.getId(),true)).isInstanceOf(BusinessException.class);
        assertThat(query.attempts(invoice)).hasSize(2);assertThat(query.booking(b.getId()).getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(tx(()->db.scalar("select count(p) from Payment p where p.invoice.id=:id",Long.class,Map.of("id",invoice.getId())))).isEqualTo(1);
        catalog.clean(room.getId());assertThat(query.get(Room.class,room.getId()).getStatus()).isEqualTo(RoomStatus.READY);
        as("admin@luxury.local");assertThat((BigDecimal)query.revenue(day,day.plusDays(1),true,0).get("total")).isEqualByComparingTo(before.add(invoice.getTotal()));
        for(String path:List.of("/bookings/"+b.getId(),"/bookings/"+b.getId()+"/invoice"))mvc.perform(get(path).with(user("customer@luxury.local").roles("CUSTOMER"))).andExpect(status().isOk());
    }
    @Test void simultaneousBookingsAllowExactlyOneAndAdjacentDatesRemainAvailable() throws Exception {
        var room=freshRoom();var gate=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)){
            Callable<Boolean> attempt=()->{gate.await();as("customer@luxury.local");try{bookings.create(request(room,day,day.plusDays(2)));return true;}catch(BusinessException expected){return false;}finally{SecurityContextHolder.clearContext();}};
            var a=executor.submit(attempt);var b=executor.submit(attempt);gate.countDown();
            assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        as("customer@luxury.local");assertThat(bookings.create(request(room,day.plusDays(2),day.plusDays(3))).getId()).isNotNull();
        var search=new Forms.Search();search.setCheckIn(day);search.setCheckOut(day.plusDays(1));search.setGuests(2);
        assertThat(bookings.search(search)).extracting(Room::getId).doesNotContain(room.getId());
    }
    @Test void simultaneousPaymentsCreateExactlyOneReceipt() throws Exception {
        var b=create(freshRoom());checkIn(b);clock.set(day.plusDays(1).atTime(12,0));bookings.transition(b.getId(),"checkout","");
        var gate=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)){
            Callable<Boolean> attempt=()->{gate.await();as("staff@luxury.local");try{bookings.pay(b.getId(),true);return true;}catch(BusinessException expected){return false;}finally{SecurityContextHolder.clearContext();}};
            var a=executor.submit(attempt);var c=executor.submit(attempt);gate.countDown();assertThat(List.of(a.get(20,TimeUnit.SECONDS),c.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        as("staff@luxury.local");assertThat(query.attempts(query.invoiceForBooking(b.getId()))).hasSize(1);
    }
    @Test void dateCapacityCancellationNoShowAndDirtyRoomRules(){
        var r=freshRoom();as("customer@luxury.local");
        assertThatThrownBy(()->bookings.create(request(r,day.minusDays(1),day))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->bookings.create(request(r,day,day))).isInstanceOf(BusinessException.class);
        var over=request(r,day,day.plusDays(1));over.setGuests(3);assertThatThrownBy(()->bookings.create(over)).isInstanceOf(BusinessException.class);
        var b=create(r);clock.set(day.atTime(13,59));bookings.cancel(b.getId());
        var c=create(r);as("staff@luxury.local");bookings.transition(c.getId(),"confirm","");
        assertThatThrownBy(()->bookings.transition(c.getId(),"checkin","")).isInstanceOf(BusinessException.class);
        clock.set(day.atTime(14,0));as("customer@luxury.local");assertThatThrownBy(()->bookings.cancel(c.getId())).isInstanceOf(BusinessException.class);
        as("staff@luxury.local");assertThatThrownBy(()->bookings.transition(c.getId(),"noshow","")).isInstanceOf(BusinessException.class);
        tx(()->{db.get(Room.class,r.getId()).setStatus(RoomStatus.DIRTY);return null;});
        assertThatThrownBy(()->bookings.transition(c.getId(),"checkin","")).isInstanceOf(BusinessException.class);
        clock.set(day.atTime(23,59));assertThatThrownBy(()->bookings.transition(c.getId(),"noshow","")).isInstanceOf(BusinessException.class);
        clock.set(day.plusDays(1).atStartOfDay());bookings.transition(c.getId(),"noshow","");
        as("customer@luxury.local");assertThat(bookings.create(request(r,day.plusDays(1),day.plusDays(2))).getId()).isNotNull();
    }
    @Test void maintenanceAndLastAdminGuards(){
        var r=freshRoom();var b=create(r);as("admin@luxury.local");var form=new Forms.Room();form.setId(r.getId());form.setNumber(r.getNumber());form.setTypeId(r.getRoomType().getId());form.setFloor(1);form.setStatus("MAINTENANCE");
        assertThatThrownBy(()->catalog.room(form)).isInstanceOf(BusinessException.class).hasMessageContaining(b.getCode());
        bookings.transition(b.getId(),"reject","Không thể đáp ứng yêu cầu");catalog.room(form);
        as("customer@luxury.local");assertThatThrownBy(()->bookings.create(request(r,day,day.plusDays(1)))).isInstanceOf(BusinessException.class);
        as("admin@luxury.local");var edit=new Forms.UserEdit();edit.setId(users.findByEmail("admin@luxury.local").orElseThrow().getId());edit.setRole("CUSTOMER");edit.setEnabled(false);
        assertThatThrownBy(()->accounts.updateUser(edit)).isInstanceOf(BusinessException.class);
    }
    @Test void immediateEarlyCheckoutKeepsBookedPriceAndFreesRemainingDates() throws Exception {
        var room=freshRoom();as("customer@luxury.local");var b=bookings.create(request(room,day,day.plusDays(3)));
        checkIn(b);var s=service();var use=new Forms.UseService();use.setServiceId(s.getId());use.setQuantity(2);bookings.addService(b.getId(),use);
        // Preview is informative; POST must calculate current service totals again.
        var preview=bookings.checkoutPreview(b.getId());assertThat(preview.early()).isTrue();assertThat(preview.total()).isEqualByComparingTo("3903703.00");
        mvc.perform(get("/staff/bookings/"+b.getId()+"/checkout").with(user("staff@luxury.local").roles("STAFF")))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Trả phòng sớm vẫn tính tiền phòng theo số đêm đã đặt.")));
        as("staff@luxury.local");use.setQuantity(1);bookings.addService(b.getId(),use);
        mvc.perform(post("/staff/bookings/"+b.getId()+"/action").with(user("staff@luxury.local").roles("STAFF")).with(csrf())
            .param("action","checkout").param("reason","Khách đổi lịch di chuyển").param("total","0").param("nightlyPrice","0").param("checkOut",day.toString()))
            .andExpect(redirectedUrl("/bookings/"+b.getId())).andExpect(flash().attributeExists("success"));
        as("staff@luxury.local");var after=query.booking(b.getId());var invoice=query.invoiceForBooking(b.getId());
        assertThat(after.getCheckIn()).isEqualTo(day);assertThat(after.getCheckOut()).isEqualTo(day.plusDays(3));assertThat(after.getNights()).isEqualTo(3);
        assertThat(after.getCheckedOutAt()).isEqualTo(after.getCheckedInAt());assertThat(after.getCheckoutReason()).isEqualTo("Khách đổi lịch di chuyển");
        assertThat(after.getCheckedOutBy().getEmail()).isEqualTo("staff@luxury.local");assertThat(invoice.getTotal()).isEqualByComparingTo("4003703.25");
        assertThat(query.items(invoice)).hasSize(3);assertThat(after.getStatus()).isEqualTo(BookingStatus.AWAITING_PAYMENT);
        var search=new Forms.Search();search.setCheckIn(day);search.setCheckOut(day.plusDays(2));search.setGuests(2);
        assertThat(bookings.search(search)).extracting(Room::getId).contains(room.getId());
        as("customer@luxury.local");var next=bookings.create(request(room,day,day.plusDays(2)));
        as("staff@luxury.local");bookings.transition(next.getId(),"confirm","");
        assertThatThrownBy(()->bookings.transition(next.getId(),"checkin","")).isInstanceOf(BusinessException.class);
        catalog.clean(room.getId());bookings.transition(next.getId(),"checkin","");
        // A stale checkout for the previous guest must not dirty the new guest's room.
        mvc.perform(post("/staff/bookings/"+b.getId()+"/action").with(user("staff@luxury.local").roles("STAFF")).with(csrf()).param("action","checkout"))
            .andExpect(redirectedUrl("/bookings/"+b.getId())).andExpect(flash().attributeExists("errorMessage"));
        as("staff@luxury.local");assertThat(query.get(Room.class,room.getId()).getStatus()).isEqualTo(RoomStatus.OCCUPIED);
        assertThatThrownBy(()->bookings.addService(b.getId(),use)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->catalog.clean(room.getId())).isInstanceOf(BusinessException.class);
        bookings.pay(b.getId(),false);assertThat(query.invoiceForBooking(b.getId()).getId()).isEqualTo(invoice.getId());
        assertThat(query.booking(b.getId()).getStatus()).isEqualTo(BookingStatus.AWAITING_PAYMENT);
        bookings.pay(b.getId(),true);assertThatThrownBy(()->bookings.pay(b.getId(),true)).isInstanceOf(BusinessException.class);
        assertThat(query.attempts(invoice)).hasSize(2);assertThat(query.invoiceForBooking(b.getId()).getId()).isEqualTo(invoice.getId());
        assertThat(query.get(Room.class,room.getId()).getStatus()).isEqualTo(RoomStatus.OCCUPIED);
        assertThat(tx(()->db.scalar("select count(i) from Invoice i where i.booking.id=:id",Long.class,Map.of("id",b.getId())))).isEqualTo(1L);
        assertThat(tx(()->db.scalar("select count(p) from Payment p where p.invoice.id=:id",Long.class,Map.of("id",invoice.getId())))).isEqualTo(1L);
    }

    @Test void concurrentCheckoutCreatesOneInvoiceAndOneTransition() throws Exception {
        var b=create(freshRoom());checkIn(b);var gate=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)){
            Callable<Boolean> attempt=()->{gate.await();as("staff@luxury.local");try{bookings.transition(b.getId(),"checkout","Trả ngay");return true;}catch(BusinessException expected){return false;}finally{SecurityContextHolder.clearContext();}};
            var first=executor.submit(attempt);var second=executor.submit(attempt);gate.countDown();
            assertThat(List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        assertThat(tx(()->db.scalar("select count(i) from Invoice i where i.booking.id=:id",Long.class,Map.of("id",b.getId())))).isEqualTo(1L);
        as("staff@luxury.local");assertThat(query.history(b.getId()).stream().filter(h->h.getToStatus().equals("AWAITING_PAYMENT"))).hasSize(1);
    }

    @Test void checkoutRejectsWrongStateMissingActualTimeAndTimeBeforeCheckIn() throws Exception {
        var b=create(freshRoom());as("staff@luxury.local");
        assertThatThrownBy(()->bookings.transition(b.getId(),"checkout","")).isInstanceOf(BusinessException.class);
        bookings.transition(b.getId(),"confirm","");assertThatThrownBy(()->bookings.transition(b.getId(),"checkout","")).isInstanceOf(BusinessException.class);
        clock.set(day.atTime(14,0));bookings.transition(b.getId(),"checkin","");
        clock.set(day.atTime(13,59));assertThatThrownBy(()->bookings.transition(b.getId(),"checkout","")).isInstanceOf(BusinessException.class).hasMessageContaining("không được trước");
        clock.set(day.atTime(14,0));
        mvc.perform(post("/staff/bookings/"+b.getId()+"/action").with(user("customer@luxury.local").roles("CUSTOMER")).with(csrf()).param("action","checkout"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/staff/bookings/"+b.getId()+"/checkout").with(user("customer@luxury.local").roles("CUSTOMER"))).andExpect(status().isForbidden());
        as("staff@luxury.local");assertThat(query.invoiceForBooking(b.getId())).isNull();
        tx(()->{db.get(Booking.class,b.getId()).setCheckedInAt(null);return null;});
        assertThatThrownBy(()->bookings.transition(b.getId(),"checkout","")).isInstanceOf(BusinessException.class).hasMessageContaining("thiếu thời điểm");
        assertThat(query.booking(b.getId()).getCheckedInAt()).isNull();
        tx(()->{db.get(Booking.class,b.getId()).setCheckedInAt(day.atTime(14,0));return null;});
        bookings.transition(b.getId(),"checkout","");bookings.pay(b.getId(),true);
        assertThatThrownBy(()->bookings.transition(b.getId(),"checkout","")).isInstanceOf(BusinessException.class);
        clock.set(day.atTime(10,0));var cancelled=create(freshRoom());bookings.cancel(cancelled.getId());as("staff@luxury.local");
        assertThatThrownBy(()->bookings.transition(cancelled.getId(),"checkout","")).isInstanceOf(BusinessException.class);
    }

    @Test void cleaningAndAdminReadyBothRejectAnExistingOccupant(){
        var room=freshRoom();var b=create(room);checkIn(b);
        // Deliberately inconsistent fixture, confined to this test's own new room.
        tx(()->{db.get(Room.class,room.getId()).setStatus(RoomStatus.DIRTY);return null;});
        assertThatThrownBy(()->catalog.clean(room.getId())).isInstanceOf(BusinessException.class).hasMessageContaining("vẫn có khách");
        as("admin@luxury.local");var form=new Forms.Room();form.setId(room.getId());form.setNumber(room.getNumber());form.setFloor(1);form.setTypeId(room.getRoomType().getId());form.setStatus("READY");
        assertThatThrownBy(()->catalog.room(form)).isInstanceOf(BusinessException.class).hasMessageContaining("vẫn có khách");
        tx(()->{db.get(Room.class,room.getId()).setStatus(RoomStatus.OCCUPIED);return null;});
    }
    @Test void httpCatalogWritesValidationAndInvoiceAccess() throws Exception {
        String suffix=UUID.randomUUID().toString().substring(0,8);
        var admin=user("admin@luxury.local").roles("ADMIN");
        mvc.perform(post("/admin/catalog/types").with(admin).with(csrf()).param("name","HTTP-Type-"+suffix).param("description","Phòng kiểm tra biểu mẫu").param("capacity","2").param("price","1250000.25").param("active","true")).andExpect(status().is3xxRedirection());
        var type=tx(()->db.list("from RoomType where name=:name",RoomType.class,Map.of("name","HTTP-Type-"+suffix)).getFirst());
        mvc.perform(post("/admin/catalog/rooms").with(admin).with(csrf()).param("number","H"+suffix).param("floor","2").param("typeId",type.getId().toString()).param("status","READY")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/catalog/amenities").with(admin).with(csrf()).param("name","HTTP-Amenity-"+suffix).param("active","true")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/catalog/services").with(admin).with(csrf()).param("name","HTTP-Service-"+suffix).param("unit","lượt").param("price","12000.50").param("active","true")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/catalog/services").with(admin).with(csrf()).param("name","Invalid-"+suffix).param("unit","lượt").param("price","-1")).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","price"));
        for(String kind:List.of("types","rooms","amenities","services")){
            Long id=tx(()->switch(kind){case "types"->type.getId();case "rooms"->db.list("from Room where number=:n",Room.class,Map.of("n","H"+suffix)).getFirst().getId();case "amenities"->db.list("from Amenity where name=:n",Amenity.class,Map.of("n","HTTP-Amenity-"+suffix)).getFirst().getId();default->db.list("from HotelService where name=:n",HotelService.class,Map.of("n","HTTP-Service-"+suffix)).getFirst().getId();});
            mvc.perform(get("/admin/catalog/"+kind+"/edit").param("id",id.toString()).with(admin)).andExpect(status().isOk());
        }
        var b=create(freshRoom());checkIn(b);clock.set(day.plusDays(1).atTime(12,0));bookings.transition(b.getId(),"checkout","");
        mvc.perform(get("/staff/invoices").with(admin)).andExpect(status().isOk());
        String outsider="outsider-"+suffix+"@example.com";var f=new Forms.Register();f.setEmail(outsider);f.setFullName("Khách khác");f.setPhone("0901234567");f.setPassword("LongPassword!2026");accounts.register(f);
        mvc.perform(get("/bookings/"+b.getId()+"/invoice").with(user(outsider).roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(post("/bookings/"+b.getId()+"/cancel").with(user(outsider).roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void authenticationAuthorizationOwnershipCsrfAndAllPageTemplates() throws Exception {
        for(String who:List.of("customer","staff","admin")){
            var login=mvc.perform(formLogin("/login").user(who+"@luxury.local").password("LuxuryDemo!2026")).andExpect(status().is3xxRedirection()).andReturn();
            var session=(MockHttpSession)login.getRequest().getSession(false);assertThat(session).isNotNull();
            mvc.perform(get("/account").session(session)).andExpect(redirectedUrl(who.equals("customer")?"/bookings":"/staff"));
            mvc.perform(get("/staff").session(session)).andExpect(who.equals("customer")?status().isForbidden():status().isOk());
            mvc.perform(get("/admin/users").session(session)).andExpect(who.equals("admin")?status().isOk():status().isForbidden());
        }
        mvc.perform(post("/profile").with(user("customer@luxury.local").roles("CUSTOMER"))).andExpect(status().isForbidden());
        String mail="IT-"+UUID.randomUUID()+"@example.com";
        mvc.perform(post("/register").with(csrf()).param("fullName","Khách độc lập").param("email",mail).param("phone","0901234567").param("password","LongTestPassword!12").param("role","ADMIN")).andExpect(status().is3xxRedirection());
        var registered=users.findByEmail(mail.toLowerCase(Locale.ROOT)).orElseThrow();assertThat(registered.getRole().getName()).isEqualTo("CUSTOMER");assertThat(registered.getPassword()).startsWith("$2");
        var b=create(freshRoom());mvc.perform(get("/bookings/"+b.getId()).with(user(registered.getEmail()).roles("CUSTOMER"))).andExpect(status().isForbidden());
        as(registered.getEmail());assertThatThrownBy(()->query.booking(b.getId())).isInstanceOf(AccessDeniedException.class);
        for(String path:List.of("/","/login","/register","/rooms?checkIn=2030-06-15&checkOut=2030-06-16&guests=2","/rooms/"+b.getRoom().getId()))mvc.perform(get(path)).andExpect(status().isOk());
        for(String path:List.of("/staff","/staff/bookings","/staff/rooms","/staff/audit","/admin/users","/admin/revenue","/profile","/bookings","/bookings/"+b.getId(),"/admin/catalog/types","/admin/catalog/rooms","/admin/catalog/amenities","/admin/catalog/services","/admin/catalog/types/edit","/admin/catalog/rooms/edit","/admin/catalog/amenities/edit","/admin/catalog/services/edit"))mvc.perform(get(path).with(user("admin@luxury.local").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/bookings/new").param("roomId",b.getRoom().getId().toString()).param("checkIn",day.toString()).param("checkOut",day.plusDays(1).toString()).param("guests","2").with(user("customer@luxury.local").roles("CUSTOMER"))).andExpect(status().isOk());
        tx(()->{db.get(UserAccount.class,registered.getId()).setEnabled(false);return null;});
        mvc.perform(get("/bookings").with(user(registered.getEmail()).roles("CUSTOMER"))).andExpect(redirectedUrl("/login?locked"));
    }
    @TestConfiguration static class TimeConfig {@Bean @Primary MutableClock mutableClock(){return new MutableClock();}}
    static class MutableClock extends Clock {
        private volatile Instant instant=LocalDate.of(2030,6,15).atTime(10,0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        void set(LocalDateTime now){instant=now.atZone(getZone()).toInstant();}
        @Override public ZoneId getZone(){return ZoneId.of("Asia/Ho_Chi_Minh");}
        @Override public Clock withZone(ZoneId zone){return Clock.fixed(instant,zone);}
        @Override public Instant instant(){return instant;}
    }
}
