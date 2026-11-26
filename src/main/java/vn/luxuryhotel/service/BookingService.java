package vn.luxuryhotel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luxuryhotel.configuration.HotelTime;
import vn.luxuryhotel.configuration.StayPolicy;
import vn.luxuryhotel.dto.CheckoutSummary;
import vn.luxuryhotel.dto.Forms;
import vn.luxuryhotel.entity.*;
import vn.luxuryhotel.exception.*;
import vn.luxuryhotel.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class BookingService {
    public static final List<BookingStatus> HOLD=List.of(BookingStatus.PENDING,BookingStatus.CONFIRMED,BookingStatus.CHECKED_IN);
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final HotelRepository db;
    private final ActorService actors;
    private final AccountService accounts;
    private final HotelTime time;
    private final StayPolicy stayPolicy;

    public void dates(LocalDate in,LocalDate out,int guests){
        if(in==null||out==null)throw new BusinessException("Vui lòng chọn ngày nhận và ngày trả.");
        if(in.isBefore(time.today()))throw new BusinessException("Ngày nhận không được trước hôm nay.");
        if(!out.isAfter(in))throw new BusinessException("Ngày trả phải sau ngày nhận.");
        if(guests<1||guests>100)throw new BusinessException("Số khách phải từ 1 đến 100.");
        if(ChronoUnit.DAYS.between(in,out)>365)throw new BusinessException("Mỗi đơn tối đa 365 đêm.");
    }
    @Transactional(readOnly=true)
    public List<Room> search(Forms.Search form){
        dates(form.getCheckIn(),form.getCheckOut(),form.getGuests());
        var p=new HashMap<String,Object>();p.put("inDate",form.getCheckIn());p.put("outDate",form.getCheckOut());p.put("guests",form.getGuests());p.put("hold",HOLD);
        var hql="select r from Room r where r.roomType.active=true and r.roomType.capacity>=:guests and r.status not in (vn.luxuryhotel.entity.RoomStatus.MAINTENANCE,vn.luxuryhotel.entity.RoomStatus.INACTIVE) and not exists(select b.id from Booking b where b.room=r and b.status in :hold and b.checkIn<:outDate and b.checkOut>:inDate)";
        if(form.getTypeId()!=null){hql+=" and r.roomType.id=:type";p.put("type",form.getTypeId());}
        return db.list(hql+" order by r.roomType.price,r.number",Room.class,p);
    }
    public Booking create(Forms.Book form){
        var customer=actors.current();
        if(!customer.getRole().getName().equals("CUSTOMER"))throw new BusinessException("Đặt phòng bằng tài khoản khách hàng.");
        dates(form.getCheckIn(),form.getCheckOut(),form.getGuests());
        var room=rooms.lockById(form.getRoomId()).orElseThrow(NotFoundException::new);
        if(!room.getStatus().bookable()||!room.getRoomType().isActive())throw new BusinessException("Phòng hiện không nhận đặt mới.");
        if(form.getGuests()>room.getRoomType().getCapacity())throw new BusinessException("Số khách vượt sức chứa phòng.");
        if(bookings.overlaps(room.getId(),form.getCheckIn(),form.getCheckOut(),HOLD)>0)throw new BusinessException("Phòng vừa có người đặt trong khoảng ngày này. Vui lòng chọn phòng hoặc ngày khác.");
        var b=new Booking();b.setCode("LH-"+UUID.randomUUID().toString().toUpperCase(Locale.ROOT));
        b.setCustomer(customer);b.setRoom(room);b.setCheckIn(form.getCheckIn());b.setCheckOut(form.getCheckOut());b.setGuests(form.getGuests());
        b.setNights(ChronoUnit.DAYS.between(form.getCheckIn(),form.getCheckOut()));b.setNightlyPrice(room.getRoomType().getPrice());
        b.setContactName(form.getContactName().trim());b.setContactPhone(form.getContactPhone());b.setContactEmail(form.getContactEmail().trim().toLowerCase(Locale.ROOT));b.setNote(form.getNote());
        b.setStatus(BookingStatus.PENDING);b.setDemo(customer.isDemo());b.setCreatedAt(time.now());bookings.save(b);
        history(b,null,customer,"Gửi yêu cầu đặt phòng");return b;
    }
    private Booking locked(Long id){
        // Read only room id before acquiring the shared mutex, then reload the booking under lock.
        Long roomId=db.scalar("select b.room.id from Booking b where b.id=:id",Long.class,Map.of("id",id));
        rooms.lockById(roomId).orElseThrow(NotFoundException::new);
        return db.lock(Booking.class,id);
    }
    private void require(Booking b,BookingStatus expected){if(b.getStatus()!=expected)throw new BusinessException("Thao tác không hợp lệ với trạng thái "+b.getStatus().getLabel()+".");}
    private void history(Booking b,BookingStatus from,UserAccount actor,String reason){
        var h=new BookingHistory();h.setBooking(b);h.setFromStatus(from==null?null:from.name());h.setToStatus(b.getStatus().name());h.setActor(actor);h.setReason(reason);h.setCreatedAt(time.now());db.add(h);
        accounts.audit(actor,"BOOKING_"+b.getStatus(),b.getCode()+": "+reason);
    }
    private void change(Booking b,BookingStatus status,UserAccount actor,String reason){var from=b.getStatus();b.setStatus(status);history(b,from,actor,reason);}
    public void cancel(Long id){
        var actor=actors.current();var b=locked(id);actors.owns(b);
        if(b.getStatus()!=BookingStatus.PENDING&&b.getStatus()!=BookingStatus.CONFIRMED)throw new BusinessException("Chỉ được hủy đơn chờ hoặc đã xác nhận.");
        if(!time.now().isBefore(b.getCheckIn().atTime(time.checkInTime)))throw new BusinessException("Đã quá hạn hủy: trước 14:00 ngày nhận phòng.");
        change(b,BookingStatus.CANCELLED,actor,"Hủy trước hạn nhận phòng");
    }
    public void transition(Long id,String action,String reason){
        var actor=actors.staff();var b=locked(id);var room=b.getRoom();
        switch(action){
            case "confirm" -> {require(b,BookingStatus.PENDING);change(b,BookingStatus.CONFIRMED,actor,"Lễ tân xác nhận");}
            case "reject" -> {require(b,BookingStatus.PENDING);if(reason==null||reason.isBlank()||reason.length()>1000)throw new BusinessException("Nhập lý do từ chối (1–1000 ký tự).");change(b,BookingStatus.REJECTED,actor,reason.trim());}
            case "checkin" -> {
                require(b,BookingStatus.CONFIRMED);
                var actual = time.now();
                stayPolicy.validateCheckIn(b,actual);
                if(room.getStatus()!=RoomStatus.READY)throw new BusinessException("Phòng phải Sẵn sàng trước khi nhận khách.");
                if(!bookings.findByRoomIdAndStatusIn(room.getId(),List.of(BookingStatus.CHECKED_IN)).isEmpty())throw new BusinessException("Phòng vẫn còn khách đang lưu trú.");
                b.setCheckedInAt(actual);room.setStatus(RoomStatus.OCCUPIED);change(b,BookingStatus.CHECKED_IN,actor,"Đã đối chiếu thông tin và nhận phòng"+(stayPolicy.isDemoEarlyCheckIn()&&actual.toLocalTime().isBefore(time.checkInTime)?" (chế độ demo: nhận trước giờ thông thường)":""));
            }
            case "noshow" -> {require(b,BookingStatus.CONFIRMED);if(!time.now().isAfter(b.getCheckIn().atTime(time.noShowTime)))throw new BusinessException("Chỉ đánh dấu không đến sau 23:59 ngày nhận.");change(b,BookingStatus.NO_SHOW,actor,"Khách không đến sau thời hạn");}
            case "checkout" -> checkout(b,actor,reason);
            default -> throw new BusinessException("Thao tác không được hỗ trợ.");
        }
    }
    public void addService(Long id,Forms.UseService form){
        var actor=actors.staff();var b=locked(id);require(b,BookingStatus.CHECKED_IN);
        if(form.getQuantity()<1||form.getQuantity()>10000)throw new BusinessException("Số lượng từ 1 đến 10000.");
        var service=db.lock(HotelService.class,form.getServiceId());
        if(!service.isActive())throw new BusinessException("Dịch vụ đã ngừng hoạt động.");
        var item=new BookingServiceItem();item.setBooking(b);item.setService(service);item.setDescription(service.getName()+" / "+service.getUnit());item.setQuantity(form.getQuantity());
        item.setUnitPrice(service.getPrice());item.setUsedAt(time.now());item.setActor(actor);db.add(item);
        accounts.audit(actor,"SERVICE_USED",b.getCode()+": "+service.getName()+" × "+form.getQuantity());
    }
    @Transactional(readOnly=true)
    public CheckoutSummary checkoutPreview(Long id) {
        actors.staff();
        var b=db.get(Booking.class,id);
        require(b,BookingStatus.CHECKED_IN);
        var actual=time.now();
        stayPolicy.validateActualCheckout(b,actual);
        var items=db.list("from BookingServiceItem where booking.id=:id order by id",BookingServiceItem.class,Map.of("id",id));
        var serviceTotal=items.stream().map(BookingServiceItem::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new CheckoutSummary(b,List.copyOf(items),actual,stayPolicy.isEarlyCheckout(b,actual),serviceTotal,b.getRoomTotal().add(serviceTotal));
    }

    private void checkout(Booking b,UserAccount actor,String reason){
        require(b,BookingStatus.CHECKED_IN);
        var actual=time.now();
        stayPolicy.validateActualCheckout(b,actual);
        String checkoutReason=reason==null?"":reason.strip();
        if(checkoutReason.length()>500)throw new BusinessException("Lý do trả phòng tối đa 500 ký tự.");
        var occupants=bookings.findByRoomIdAndStatusIn(b.getRoom().getId(),List.of(BookingStatus.CHECKED_IN));
        if(occupants.stream().anyMatch(other->!other.getId().equals(b.getId()))) {
            throw new BusinessException("Phòng còn một lượt lưu trú khác đang sử dụng. Hãy kiểm tra dữ liệu trước khi trả phòng.");
        }
        // locked() acquired room then booking locks. A repeated/stale POST fails require()
        // before touching the room, including when the next guest has already checked in.
        var services=db.list("from BookingServiceItem where booking.id=:id order by id",BookingServiceItem.class,Map.of("id",b.getId()));
        var inv=new Invoice();inv.setBooking(b);inv.setCode("INV-"+UUID.randomUUID().toString().toUpperCase(Locale.ROOT));inv.setCustomerName(b.getContactName());inv.setCustomerEmail(b.getContactEmail());inv.setRoomNumber(b.getRoom().getNumber());
        inv.setRoomTotal(b.getRoomTotal());inv.setServiceTotal(services.stream().map(BookingServiceItem::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add));inv.setTotal(inv.getRoomTotal().add(inv.getServiceTotal()));inv.setIssuedAt(actual);db.add(inv);
        invoiceItem(inv,"Phòng "+b.getRoom().getNumber()+" · "+b.getCheckIn()+" → "+b.getCheckOut(),b.getNights(),b.getNightlyPrice());
        for(var s:services)invoiceItem(inv,s.getDescription(),s.getQuantity(),s.getUnitPrice());
        b.setCheckedOutAt(actual);b.setCheckedOutBy(actor);b.setCheckoutReason(checkoutReason.isBlank()?null:checkoutReason);
        b.getRoom().setStatus(RoomStatus.DIRTY);
        String detail=stayPolicy.isEarlyCheckout(b,actual)
            ?"Trả phòng sớm; vẫn tính đủ "+b.getNights()+" đêm đã đặt."
            :"Trả phòng, chốt hóa đơn.";
        if(actual.isAfter(b.getCheckOut().atTime(time.checkOutTime)))detail+=" Trả muộn; chưa tính phụ thu.";
        if(!checkoutReason.isBlank())detail+=" Lý do: "+checkoutReason;
        change(b,BookingStatus.AWAITING_PAYMENT,actor,detail);
    }
    private void invoiceItem(Invoice inv,String description,long quantity,BigDecimal price){
        var item=new InvoiceItem();item.setInvoice(inv);item.setDescription(description);item.setQuantity(quantity);item.setUnitPrice(price);item.setTotal(price.multiply(BigDecimal.valueOf(quantity)));db.add(item);
    }
    public void pay(Long bookingId,boolean success){
        var actor=actors.staff();var b=locked(bookingId);require(b,BookingStatus.AWAITING_PAYMENT);
        var inv=db.list("from Invoice where booking.id=:id",Invoice.class,Map.of("id",b.getId())).getFirst();
        if(inv.isPaid())throw new BusinessException("Hóa đơn đã được thanh toán.");
        var attempt=new PaymentAttempt();attempt.setInvoice(inv);attempt.setActor(actor);attempt.setSuccessful(success);attempt.setAttemptedAt(time.now());attempt.setReference("SIM-"+UUID.randomUUID());db.add(attempt);
        if(success){
            var payment=new Payment();payment.setInvoice(inv);payment.setAttempt(attempt);payment.setAmount(inv.getTotal());payment.setPaidAt(time.now());db.add(payment);
            inv.setPaid(true);change(b,BookingStatus.COMPLETED,actor,"Thanh toán mô phỏng thành công");
        }else accounts.audit(actor,"PAYMENT_FAILED",b.getCode()+": thanh toán mô phỏng thất bại, có thể thử lại");
    }
}
