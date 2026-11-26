package vn.luxuryhotel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.dao.*;
import org.springframework.http.HttpStatus;
import vn.luxuryhotel.service.*;
import vn.luxuryhotel.configuration.HotelTime;
import vn.luxuryhotel.configuration.StayPolicy;
import vn.luxuryhotel.exception.*;
import vn.luxuryhotel.entity.*;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice @RequiredArgsConstructor
public class CommonAdvice {
    private final ActorService actors;
    private final HotelTime time;
    private final StayPolicy stayPolicy;
    @ModelAttribute public void common(Model model,Authentication auth){
        model.addAttribute("today",time.today());model.addAttribute("now",time.now());
        model.addAttribute("demoEarlyCheckIn",stayPolicy.isDemoEarlyCheckIn());
        model.addAttribute("checkInTime",time.checkInTime);model.addAttribute("checkOutTime",time.checkOutTime);model.addAttribute("noShowTime",time.noShowTime);
        model.addAttribute("bookingStatuses",BookingStatus.values());model.addAttribute("roomStatuses",RoomStatus.values());
        if(auth!=null&&auth.isAuthenticated()){
            var u=actors.current();model.addAttribute("currentUser",u);model.addAttribute("staff",actors.isStaff(u));model.addAttribute("admin",u.getRole().getName().equals("ADMIN"));
        }else{model.addAttribute("staff",false);model.addAttribute("admin",false);}
    }
    @ExceptionHandler(BusinessException.class) public String business(BusinessException ex,Model m,HttpServletResponse response){
        response.setStatus(409);m.addAttribute("errorMessage",ex.getMessage());return "error/business";
    }
    @ExceptionHandler({DataIntegrityViolationException.class,ConcurrencyFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT) public String conflict(Model m){m.addAttribute("errorMessage","Dữ liệu vừa thay đổi hoặc bị trùng. Vui lòng tải lại và kiểm tra trước khi thử lại.");return "error/business";}
    @ExceptionHandler({IllegalArgumentException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST) public String invalid(Model m){m.addAttribute("errorMessage","Bộ lọc hoặc dữ liệu gửi lên không hợp lệ.");return "error/business";}
    @ExceptionHandler(jakarta.persistence.NoResultException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(){return "error/404";}
}
