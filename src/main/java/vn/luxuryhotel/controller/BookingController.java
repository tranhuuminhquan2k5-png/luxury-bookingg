package vn.luxuryhotel.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luxuryhotel.service.*;
import vn.luxuryhotel.dto.Forms;
import vn.luxuryhotel.entity.Room;
import vn.luxuryhotel.exception.BusinessException;
import java.time.LocalDate;

@Controller @RequiredArgsConstructor
public class BookingController {
    private final QueryService query;private final BookingService service;private final ActorService actors;
    @GetMapping("/bookings/new") public String book(@RequestParam Long roomId,@RequestParam LocalDate checkIn,@RequestParam LocalDate checkOut,@RequestParam int guests,Model m){
        service.dates(checkIn,checkOut,guests);var u=actors.current();var f=new Forms.Book();f.setRoomId(roomId);f.setCheckIn(checkIn);f.setCheckOut(checkOut);f.setGuests(guests);
        f.setContactName(u.getFullName());f.setContactPhone(u.getPhone());f.setContactEmail(u.getEmail());m.addAttribute("form",f);m.addAttribute("room",query.get(Room.class,roomId));return "book";
    }
    @PostMapping("/bookings") public String create(@Valid @ModelAttribute("form") Forms.Book form,BindingResult errors,Model m,RedirectAttributes flash){
        if(!errors.hasErrors())try{var b=service.create(form);flash.addFlashAttribute("success","Đã gửi yêu cầu. Phòng được giữ lịch trong khi chờ xác nhận.");return "redirect:/bookings/"+b.getId();}catch(BusinessException e){errors.reject("booking",e.getMessage());}
        m.addAttribute("room",query.get(Room.class,form.getRoomId()));return "book";
    }
    @GetMapping("/bookings") public String list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="0") int page,Model m){
        m.addAttribute("result",query.bookings(false,q,status,null,null,page));m.addAttribute("q",q);m.addAttribute("status",status);m.addAttribute("listPath","/bookings");return "bookings";
    }
    @GetMapping("/bookings/{id}") public String detail(@PathVariable Long id,Model m){
        var b=query.booking(id);m.addAttribute("booking",b);m.addAttribute("history",query.history(id));m.addAttribute("usedServices",query.usedServices(id));
        m.addAttribute("invoice",query.invoiceForBooking(id));m.addAttribute("useForm",new Forms.UseService());if(actors.isStaff(actors.current()))m.addAttribute("services",query.services());return "booking";
    }
    @PostMapping("/bookings/{id}/cancel") public String cancel(@PathVariable Long id,RedirectAttributes f){
        try{service.cancel(id);f.addFlashAttribute("success","Đã hủy đơn và giải phóng lịch phòng.");}
        catch(BusinessException e){f.addFlashAttribute("errorMessage",e.getMessage());}
        return "redirect:/bookings/"+id;
    }
    @GetMapping("/bookings/{id}/invoice") public String invoice(@PathVariable Long id,Model m){
        var inv=query.invoiceForBooking(id);if(inv==null)throw new vn.luxuryhotel.exception.NotFoundException();
        m.addAttribute("invoice",inv);m.addAttribute("items",query.items(inv));m.addAttribute("attempts",query.attempts(inv));return "invoice";
    }
}
