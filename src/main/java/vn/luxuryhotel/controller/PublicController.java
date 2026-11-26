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
import vn.luxuryhotel.configuration.HotelTime;
import java.util.List;

@Controller @RequiredArgsConstructor
public class PublicController {
    private final QueryService query;private final BookingService bookings;private final AccountService accounts;private final ActorService actors;private final HotelTime time;
    @GetMapping("/") public String home(Model m){var s=new Forms.Search();s.setCheckIn(time.today());s.setCheckOut(time.today().plusDays(1));m.addAttribute("search",s);m.addAttribute("types",query.types());return "home";}
    @GetMapping("/rooms") public String rooms(@Valid @ModelAttribute("search") Forms.Search search,BindingResult errors,Model m){
        m.addAttribute("types",query.types());m.addAttribute("rooms",List.of());
        if(!errors.hasErrors())try{m.addAttribute("rooms",bookings.search(search));}catch(BusinessException e){errors.reject("dates",e.getMessage());}
        return "rooms";
    }
    @GetMapping("/rooms/{id}") public String room(@PathVariable Long id,@ModelAttribute("search") Forms.Search search,Model m){
        if(search.getCheckIn()==null)search.setCheckIn(time.today());if(search.getCheckOut()==null)search.setCheckOut(time.today().plusDays(1));
        m.addAttribute("room",query.get(Room.class,id));return "room";
    }
    @GetMapping("/login") public String login(){return "login";}
    @GetMapping("/register") public String register(Model m){m.addAttribute("form",new Forms.Register());return "register";}
    @PostMapping("/register") public String register(@Valid @ModelAttribute("form") Forms.Register form,BindingResult errors){
        if(errors.hasErrors())return "register";
        try{accounts.register(form);}catch(BusinessException e){errors.rejectValue("email","duplicate",e.getMessage());return "register";}
        return "redirect:/login?registered";
    }
    @GetMapping("/account") public String account(){return actors.isStaff(actors.current())?"redirect:/staff":"redirect:/bookings";}
    @GetMapping("/profile") public String profile(Model m){var u=actors.current();var f=new Forms.Profile();f.setFullName(u.getFullName());f.setPhone(u.getPhone());f.setAddress(u.getAddress());m.addAttribute("form",f);return "profile";}
    @PostMapping("/profile") public String profile(@Valid @ModelAttribute("form") Forms.Profile form,BindingResult errors,RedirectAttributes flash){
        if(errors.hasErrors())return "profile";accounts.profile(form);flash.addFlashAttribute("success","Đã cập nhật hồ sơ.");return "redirect:/profile";
    }
    @GetMapping("/error/403") public String forbidden(jakarta.servlet.http.HttpServletResponse response){response.setStatus(403);return "error/403";}
}
