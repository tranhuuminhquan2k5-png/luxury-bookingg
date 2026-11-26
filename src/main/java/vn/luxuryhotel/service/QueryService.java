package vn.luxuryhotel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import vn.luxuryhotel.entity.*;
import vn.luxuryhotel.repository.HotelRepository;
import vn.luxuryhotel.configuration.HotelTime;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class QueryService {
    private final HotelRepository db;
    private final ActorService actors;
    private final HotelTime time;
    public <T> T get(Class<T> type,Long id){return db.get(type,id);}
    public List<RoomType> types(){return db.list("from RoomType where active=true order by price",RoomType.class,Map.of());}
    public List<HotelService> services(){actors.staff();return db.list("from HotelService where active=true order by name",HotelService.class,Map.of());}
    public <T> List<T> catalog(Class<T> type){actors.admin();return db.all(type);}
    public List<Room> rooms(String q,String status){
        actors.staff();var p=new HashMap<String,Object>();p.put("q","%"+(q==null?"":q.trim().toLowerCase(Locale.ROOT))+"%");
        String hql="from Room where lower(number) like :q";
        if(status!=null&&!status.isBlank()){hql+=" and status=:status";p.put("status",RoomStatus.valueOf(status));}
        return db.list(hql+" order by number",Room.class,p);
    }
    public Page<Booking> bookings(boolean staff,String q,String status,LocalDate date,String dateKind,int page){
        var p=new HashMap<String,Object>();String where=" where 1=1";
        if(staff)actors.staff();else{where+=" and b.customer.id=:customer";p.put("customer",actors.current().getId());}
        if(q!=null&&!q.isBlank()){where+=" and (lower(b.code) like :q or lower(b.contactName) like :q or lower(b.contactEmail) like :q or b.room.number like :q)";p.put("q","%"+q.trim().toLowerCase(Locale.ROOT)+"%");}
        if(status!=null&&!status.isBlank()){where+=" and b.status=:status";p.put("status",BookingStatus.valueOf(status));}
        if(date!=null){where+=("out".equals(dateKind)?" and b.checkOut=:date":" and b.checkIn=:date");p.put("date",date);}
        int safe=Math.max(0,Math.min(page,100000));
        return new PageImpl<>(db.page("select b from Booking b"+where+" order by b.createdAt desc,b.id desc",Booking.class,p,safe,10),PageRequest.of(safe,10),db.scalar("select count(b) from Booking b"+where,Long.class,p));
    }
    public Booking booking(Long id){var b=db.get(Booking.class,id);actors.owns(b);return b;}
    public List<BookingHistory> history(Long id){booking(id);return db.list("from BookingHistory where booking.id=:id order by id",BookingHistory.class,Map.of("id",id));}
    public List<BookingServiceItem> usedServices(Long id){booking(id);return db.list("from BookingServiceItem where booking.id=:id order by id",BookingServiceItem.class,Map.of("id",id));}
    public Invoice invoiceForBooking(Long id){booking(id);return db.list("from Invoice where booking.id=:id",Invoice.class,Map.of("id",id)).stream().findFirst().orElse(null);}
    public List<InvoiceItem> items(Invoice invoice){actors.owns(invoice.getBooking());return db.list("from InvoiceItem where invoice.id=:id order by id",InvoiceItem.class,Map.of("id",invoice.getId()));}
    public List<PaymentAttempt> attempts(Invoice invoice){actors.owns(invoice.getBooking());return db.list("from PaymentAttempt where invoice.id=:id order by id desc",PaymentAttempt.class,Map.of("id",invoice.getId()));}
    public Map<String,Long> dashboard(){
        actors.staff();var p=Map.<String,Object>of("today",time.today());var counts=new LinkedHashMap<String,Long>();
        counts.put("arrivals",db.scalar("select count(b) from Booking b where b.checkIn=:today and b.status in (vn.luxuryhotel.entity.BookingStatus.CONFIRMED,vn.luxuryhotel.entity.BookingStatus.PENDING)",Long.class,p));
        counts.put("departures",db.scalar("select count(b) from Booking b where b.checkOut=:today and b.status=vn.luxuryhotel.entity.BookingStatus.CHECKED_IN",Long.class,p));
        counts.put("pending",db.scalar("select count(b) from Booking b where b.status=vn.luxuryhotel.entity.BookingStatus.PENDING",Long.class,Map.of()));
        counts.put("occupied",db.scalar("select count(r) from Room r where r.status=vn.luxuryhotel.entity.RoomStatus.OCCUPIED",Long.class,Map.of()));
        counts.put("dirty",db.scalar("select count(r) from Room r where r.status=vn.luxuryhotel.entity.RoomStatus.DIRTY",Long.class,Map.of()));
        counts.put("rooms",db.scalar("select count(r) from Room r",Long.class,Map.of()));return counts;
    }
    public Page<Invoice> invoices(String q,String state,int page){
        actors.staff();var p=new HashMap<String,Object>();p.put("q","%"+(q==null?"":q.toLowerCase(Locale.ROOT))+"%");
        String where=" where (lower(i.code) like :q or lower(i.customerName) like :q or lower(i.booking.code) like :q)";
        if("paid".equals(state)||"unpaid".equals(state)){where+=" and i.paid=:paid";p.put("paid","paid".equals(state));}
        int safe=Math.max(0,Math.min(page,100000));
        return new PageImpl<>(db.page("select i from Invoice i"+where+" order by i.id desc",Invoice.class,p,safe,15),PageRequest.of(safe,15),db.scalar("select count(i) from Invoice i"+where,Long.class,p));
    }
    public Page<AuditLog> audit(String q,int page){
        actors.staff();var p=Map.<String,Object>of("q","%"+(q==null?"":q.toLowerCase(Locale.ROOT))+"%");String where=" where lower(detail) like :q or lower(action) like :q";
        return new PageImpl<>(db.page("from AuditLog"+where+" order by id desc",AuditLog.class,p,Math.max(0,page),20),PageRequest.of(Math.max(0,page),20),db.scalar("select count(a) from AuditLog a"+where,Long.class,p));
    }
    public Page<UserAccount> users(String q,int page){
        actors.admin();var p=Map.<String,Object>of("q","%"+(q==null?"":q.toLowerCase(Locale.ROOT))+"%");String where=" where lower(email) like :q or lower(fullName) like :q";
        return new PageImpl<>(db.page("from UserAccount"+where+" order by id",UserAccount.class,p,Math.max(0,page),15),PageRequest.of(Math.max(0,page),15),db.scalar("select count(u) from UserAccount u"+where,Long.class,p));
    }
    public Map<String,Object> revenue(LocalDate from,LocalDate to,boolean demo,int page){
        actors.admin();if(from==null)from=time.today().withDayOfMonth(1);if(to==null)to=time.today();
        if(to.isBefore(from))throw new vn.luxuryhotel.exception.BusinessException("Ngày kết thúc phải từ ngày bắt đầu.");
        var p=Map.<String,Object>of("fromDate",from.atStartOfDay(),"toDate",to.plusDays(1).atStartOfDay(),"demo",demo);
        String where=" where p.paidAt>=:fromDate and p.paidAt<:toDate and p.invoice.booking.demo=:demo";
        var total=db.scalar("select coalesce(sum(p.amount),0) from Payment p"+where,BigDecimal.class,p);
        var count=db.scalar("select count(p) from Payment p"+where,Long.class,p);
        var data=db.page("select p from Payment p"+where+" order by p.paidAt desc",Payment.class,p,Math.max(0,page),20);
        return Map.of("from",from,"to",to,"demo",demo,"total",total,"payments",new PageImpl<>(data,PageRequest.of(Math.max(0,page),20),count));
    }
}
