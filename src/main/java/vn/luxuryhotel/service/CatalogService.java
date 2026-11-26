package vn.luxuryhotel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.luxuryhotel.dto.Forms;
import vn.luxuryhotel.entity.*;
import vn.luxuryhotel.repository.*;
import vn.luxuryhotel.exception.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class CatalogService {
    private final HotelRepository db;
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final ActorService actors;
    private final AccountService accounts;
    private static final List<BookingStatus> HOLD=List.of(BookingStatus.PENDING,BookingStatus.CONFIRMED,BookingStatus.CHECKED_IN);
    private void lock(){actors.admin();db.lock(SystemLock.class,1L);}
    private void noHeldBookings(Room room){
        var held=bookings.findByRoomIdAndStatusIn(room.getId(),HOLD);
        if(!held.isEmpty())throw new BusinessException("Phòng "+room.getNumber()+" còn đơn giữ lịch: "+String.join(", ",held.stream().map(Booking::getCode).toList())+". Hãy xử lý các đơn trước.");
    }
    private void noCurrentStay(Room room){
        if(!bookings.findByRoomIdAndStatusIn(room.getId(),List.of(BookingStatus.CHECKED_IN)).isEmpty())
            throw new BusinessException("Phòng vẫn có khách đang lưu trú; chưa thể chuyển Sẵn sàng.");
    }
    private void unique(Class<?> entity,String property,String value,Long id){
        var params=new HashMap<String,Object>();params.put("value",value);params.put("id",id==null?-1L:id);
        if(db.scalar("select count(x) from "+entity.getSimpleName()+" x where x."+property+"=:value and x.id<>:id",Long.class,params)>0)
            throw new BusinessException("Tên hoặc số phòng đã được sử dụng.");
    }
    public void type(Forms.Type form){
        lock();unique(RoomType.class,"name",form.getName().trim(),form.getId());
        var t=form.getId()==null?new RoomType():db.get(RoomType.class,form.getId());
        if(form.getId()!=null){
            var related=db.list("from Room where roomType.id=:id order by id",Room.class,Map.of("id",t.getId()));
            for(var r:related){rooms.lockById(r.getId()).orElseThrow(NotFoundException::new);
                if(!form.isActive()||form.getCapacity()!=t.getCapacity())noHeldBookings(r);
            }
        }
        t.setName(form.getName().trim());t.setDescription(form.getDescription());t.setCapacity(form.getCapacity());t.setPrice(form.getPrice());t.setActive(form.isActive());
        var amenities=new LinkedHashSet<Amenity>();for(var id:form.getAmenityIds())amenities.add(db.get(Amenity.class,id));t.setAmenities(amenities);
        if(t.getId()==null)db.add(t);accounts.audit(actors.admin(),"ROOM_TYPE","Cập nhật loại phòng "+t.getName());
    }
    public void room(Forms.Room form){
        lock();unique(Room.class,"number",form.getNumber().trim().toUpperCase(Locale.ROOT),form.getId());
        var r=form.getId()==null?new Room():rooms.lockById(form.getId()).orElseThrow(NotFoundException::new);
        RoomStatus status;
        try{status=RoomStatus.valueOf(form.getStatus());}catch(Exception e){throw new BusinessException("Trạng thái phòng không hợp lệ.");}
        if(status==RoomStatus.OCCUPIED && (r.getId()==null||r.getStatus()!=RoomStatus.OCCUPIED))throw new BusinessException("Trạng thái Có khách chỉ được tạo bởi thao tác nhận phòng.");
        if(r.getId()!=null){
            if(status==RoomStatus.READY)noCurrentStay(r);
            if(r.getStatus()==RoomStatus.OCCUPIED && status!=RoomStatus.OCCUPIED)throw new BusinessException("Phải trả phòng trước khi đổi trạng thái.");
            if(!status.bookable()||!r.getRoomType().getId().equals(form.getTypeId())||!r.getNumber().equalsIgnoreCase(form.getNumber().trim()))noHeldBookings(r);
        }
        var type=db.get(RoomType.class,form.getTypeId());
        if(!type.isActive()&&status.bookable())throw new BusinessException("Loại phòng đã ngừng hoạt động.");
        r.setNumber(form.getNumber().trim().toUpperCase(Locale.ROOT));r.setFloor(form.getFloor());r.setRoomType(type);r.setStatus(status);
        if(r.getId()==null)db.add(r);accounts.audit(actors.admin(),"ROOM","Phòng "+r.getNumber()+" → "+status.getLabel());
    }
    public void amenity(Forms.Amenity form){
        lock();unique(Amenity.class,"name",form.getName().trim(),form.getId());
        var a=form.getId()==null?new Amenity():db.get(Amenity.class,form.getId());a.setName(form.getName().trim());a.setActive(form.isActive());
        if(a.getId()==null)db.add(a);accounts.audit(actors.admin(),"AMENITY","Cập nhật tiện nghi "+a.getName());
    }
    public void service(Forms.Service form){
        lock();unique(HotelService.class,"name",form.getName().trim(),form.getId());
        var s=form.getId()==null?new HotelService():db.lock(HotelService.class,form.getId());
        s.setName(form.getName().trim());s.setUnit(form.getUnit());s.setPrice(form.getPrice());s.setActive(form.isActive());
        if(s.getId()==null)db.add(s);accounts.audit(actors.admin(),"SERVICE","Cập nhật dịch vụ "+s.getName());
    }
    public void clean(Long roomId){
        var actor=actors.staff();var r=rooms.lockById(roomId).orElseThrow(NotFoundException::new);
        if(r.getStatus()!=RoomStatus.DIRTY)throw new BusinessException("Chỉ phòng Chờ dọn mới được xác nhận đã dọn.");
        noCurrentStay(r);
        r.setStatus(RoomStatus.READY);accounts.audit(actor,"ROOM_CLEAN","Đã dọn phòng "+r.getNumber());
    }
}
