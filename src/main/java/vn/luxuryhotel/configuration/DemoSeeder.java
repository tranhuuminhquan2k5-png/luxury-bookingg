package vn.luxuryhotel.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import vn.luxuryhotel.entity.*;
import vn.luxuryhotel.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Component @Profile("demo") @RequiredArgsConstructor
public class DemoSeeder implements CommandLineRunner {
    private final HotelRepository db;private final UserRepository users;private final RoomRepository rooms;private final BookingRepository bookings;private final PasswordEncoder encoder;private final HotelTime time;
    @Override @Transactional public void run(String...args){
        db.lock(SystemLock.class,1L);
        user("admin@luxury.local","Quản trị Luxury","ADMIN");user("staff@luxury.local","Lễ tân Luxury","STAFF");var customer=user("customer@luxury.local","Nguyễn Minh Anh","CUSTOMER");
        for(var name:List.of("Wi-Fi miễn phí","Điều hòa","Smart TV","Bồn tắm","Ban công","Bữa sáng"))
            if(db.list("from Amenity where name=:name",Amenity.class,Map.of("name",name)).isEmpty()){var a=new Amenity();a.setName(name);db.add(a);}
        var names=List.of("Deluxe Garden","Premier City","Executive Suite","Presidential Suite");var prices=List.of("1200000","1800000","3200000","6500000");
        var descriptions=List.of("Không gian ấm áp nhìn ra khu vườn, giường đôi êm ái và ánh sáng tự nhiên. Lựa chọn thư thái cho kỳ nghỉ của bạn.","Tầm nhìn thành phố, nội thất thanh lịch, khu vực làm việc và phòng tắm rộng rãi.","Phòng suite với khu tiếp khách riêng, ban công thoáng đãng và bồn tắm. Không gian dành cho gia đình.","Hạng phòng cao cấp nhất với phòng khách rộng, nội thất tinh tế và tầm nhìn toàn cảnh thành phố.");
        for(int i=0;i<4;i++){
            int index=i;var type=db.list("from RoomType where name=:name",RoomType.class,Map.of("name",names.get(i))).stream().findFirst().orElseGet(()->{
                var t=new RoomType();t.setName(names.get(index));t.setDescription(descriptions.get(index));t.setCapacity(index<2?2:4);t.setPrice(new BigDecimal(prices.get(index)));
                t.setAmenities(new LinkedHashSet<>(db.all(Amenity.class).subList(0,index<2?3:6)));return db.add(t);
            });
            for(int j=1;j<=5;j++){String number=""+(i+1)+"0"+j;if(rooms.findByNumber(number).isEmpty()){var r=new Room();r.setNumber(number);r.setFloor(i+1);r.setRoomType(type);db.add(r);}}
        }
        var serviceNames=List.of("Bữa sáng buffet","Đưa đón sân bay","Giặt ủi","Spa thư giãn","Minibar","Ăn tối tại phòng");var servicePrices=List.of("200000","450000","80000","600000","50000","350000");
        for(int i=0;i<6;i++)if(db.list("from HotelService where name=:name",HotelService.class,Map.of("name",serviceNames.get(i))).isEmpty()){
            var s=new HotelService();s.setName(serviceNames.get(i));s.setUnit(i==2?"kg":"lượt");s.setPrice(new BigDecimal(servicePrices.get(i)));db.add(s);
        }
        example("DEMO-PENDING",customer,"101",time.today().plusDays(2),BookingStatus.PENDING);
        example("DEMO-CONFIRMED",customer,"201",time.today(),BookingStatus.CONFIRMED);
        example("DEMO-STAY",customer,"301",time.today().minusDays(1),BookingStatus.CHECKED_IN);
        todayExample(customer);
    }
    private UserAccount user(String email,String name,String role){return users.findByEmail(email).orElseGet(()->{
        var u=new UserAccount();u.setEmail(email);u.setFullName(name);u.setPhone("0901234567");u.setPassword(encoder.encode("LuxuryDemo!2026"));u.setRole(db.list("from Role where name=:name",Role.class,Map.of("name",role)).getFirst());u.setDemo(true);u.setCreatedAt(time.now());return users.save(u);
    });}
    private void example(String code,UserAccount customer,String roomNumber,LocalDate date,BookingStatus status){
        example(code,customer,roomNumber,date,status,1);
    }
    private void todayExample(UserAccount customer){
        String code="DEMO-TODAY-"+time.today();
        if(bookings.existsByCode(code)||!customer.isDemo()||!customer.isEnabled()||!customer.getRole().getName().equals("CUSTOMER"))return;
        for(var candidate:db.all(Room.class)){
            var room=rooms.lockById(candidate.getId()).orElseThrow();
            if(room.getStatus()!=RoomStatus.READY||!room.getRoomType().isActive()||room.getRoomType().getCapacity()<2)continue;
            if(!bookings.findByRoomIdAndStatusIn(room.getId(),List.of(BookingStatus.CHECKED_IN)).isEmpty())continue;
            if(bookings.overlaps(room.getId(),time.today(),time.today().plusDays(2),vn.luxuryhotel.service.BookingService.HOLD)>0)continue;
            example(code,customer,room.getNumber(),time.today(),BookingStatus.PENDING,2);
            return;
        }
        // No available room: leave existing bookings and operational statuses untouched.
    }
    private void example(String code,UserAccount customer,String roomNumber,LocalDate date,BookingStatus status,int nights){
        if(bookings.existsByCode(code))return;
        var room=rooms.findByNumber(roomNumber).orElseThrow();rooms.lockById(room.getId()).orElseThrow();
        if(!room.getStatus().bookable()||!room.getRoomType().isActive())return;
        if(bookings.overlaps(room.getId(),date,date.plusDays(nights),vn.luxuryhotel.service.BookingService.HOLD)>0)return;
        if(status==BookingStatus.CHECKED_IN&&room.getStatus()!=RoomStatus.READY)return;
        var b=new Booking();b.setCode(code);b.setCustomer(customer);b.setRoom(room);b.setCheckIn(date);b.setCheckOut(date.plusDays(nights));b.setGuests(2);b.setNights(nights);b.setNightlyPrice(room.getRoomType().getPrice());
        b.setContactName(customer.getFullName());b.setContactEmail(customer.getEmail());b.setContactPhone(customer.getPhone());b.setDemo(true);b.setCreatedAt(time.now());b.setStatus(BookingStatus.PENDING);db.add(b);seedHistory(b,null,"Dữ liệu demo: tạo yêu cầu");
        if(status!=BookingStatus.PENDING){b.setStatus(BookingStatus.CONFIRMED);seedHistory(b,BookingStatus.PENDING,"Dữ liệu demo: xác nhận");}
        if(status==BookingStatus.CHECKED_IN){b.setStatus(status);b.setCheckedInAt(date.atTime(time.checkInTime));room.setStatus(RoomStatus.OCCUPIED);seedHistory(b,BookingStatus.CONFIRMED,"Dữ liệu demo: đã nhận phòng");}
    }
    private void seedHistory(Booking b,BookingStatus from,String reason){var h=new BookingHistory();h.setBooking(b);h.setActor(b.getCustomer());h.setFromStatus(from==null?null:from.name());h.setToStatus(b.getStatus().name());h.setCreatedAt(time.now());h.setReason(reason);db.add(h);}
}
