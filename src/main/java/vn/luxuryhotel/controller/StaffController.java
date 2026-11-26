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
import vn.luxuryhotel.exception.BusinessException;
import java.time.LocalDate;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;

@Controller @RequiredArgsConstructor @RequestMapping("/staff")
public class StaffController {
    private final QueryService query;private final BookingService service;private final CatalogService catalog;
    @GetMapping({"","/"}) public String dashboard(Model m){m.addAttribute("counts",query.dashboard());m.addAttribute("pending",query.bookings(true,"","PENDING",null,null,0));return "staff/dashboard";}
    @GetMapping("/bookings") public String bookings(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String status,
         @RequestParam(required=false) LocalDate date,@RequestParam(defaultValue="in") String dateKind,@RequestParam(defaultValue="0") int page,Model m){
        m.addAttribute("result",query.bookings(true,q,status,date,dateKind,page));m.addAttribute("q",q);m.addAttribute("status",status);m.addAttribute("date",date);m.addAttribute("dateKind",dateKind);m.addAttribute("listPath","/staff/bookings");return "bookings";
    }
    @PostMapping("/bookings/{id}/action") public String action(@PathVariable Long id,@RequestParam String action,@RequestParam(defaultValue="") String reason,RedirectAttributes f){
        try {
            service.transition(id,action,reason);
            f.addFlashAttribute("success",action.equals("checkout")?"Đã trả phòng và chốt hóa đơn theo số đêm đã đặt. Phòng đang chờ dọn.":"Đã cập nhật trạng thái và lưu lịch sử.");
        } catch(BusinessException e) {
            f.addFlashAttribute("errorMessage",e.getMessage());
            if(action.equals("checkout"))f.addFlashAttribute("checkoutReason",reason.length()<=500?reason:reason.substring(0,500));
        } catch(ConcurrencyFailureException|DataIntegrityViolationException e) {
            f.addFlashAttribute("errorMessage","Đơn vừa được xử lý hoặc dữ liệu đã thay đổi. Hãy kiểm tra trạng thái và hóa đơn hiện tại trước khi thử lại.");
        }
        return "redirect:/bookings/"+id;
    }
    @GetMapping("/bookings/{id}/checkout") public String checkout(@PathVariable Long id,Model m,RedirectAttributes f){
        try { m.addAttribute("summary",service.checkoutPreview(id)); return "staff/checkout"; }
        catch(BusinessException e){f.addFlashAttribute("errorMessage",e.getMessage());return "redirect:/bookings/"+id;}
    }
    @PostMapping("/bookings/{id}/services") public String use(@PathVariable Long id,@Valid @ModelAttribute Forms.UseService form,BindingResult errors,RedirectAttributes f){
        if(errors.hasErrors())f.addFlashAttribute("errorMessage","Chọn dịch vụ hợp lệ và số lượng từ 1 đến 10000.");
        else try {service.addService(id,form);f.addFlashAttribute("success","Đã ghi nhận dịch vụ theo đơn giá hiện tại.");}
        catch(BusinessException e){f.addFlashAttribute("errorMessage",e.getMessage());}
        return "redirect:/bookings/"+id;
    }
    @PostMapping("/bookings/{id}/pay") public String pay(@PathVariable Long id,@RequestParam String outcome,RedirectAttributes f){
        try {
            if(!outcome.equals("success")&&!outcome.equals("failed"))throw new BusinessException("Kết quả mô phỏng không hợp lệ.");
            service.pay(id,outcome.equals("success"));f.addFlashAttribute("success",outcome.equals("success")?"Thanh toán mô phỏng thành công. Đơn đã hoàn tất.":"Đã ghi nhận lần thanh toán thất bại. Có thể thử lại.");
            return "redirect:/bookings/"+id+"/invoice";
        } catch(BusinessException e){f.addFlashAttribute("errorMessage",e.getMessage());}
        catch(ConcurrencyFailureException|DataIntegrityViolationException e){f.addFlashAttribute("errorMessage","Thanh toán vừa được xử lý. Hãy kiểm tra hóa đơn và lịch sử thanh toán trước khi thử lại.");}
        return "redirect:/bookings/"+id;
    }
    @GetMapping("/rooms") public String rooms(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String status,Model m){m.addAttribute("rooms",query.rooms(q,status));m.addAttribute("q",q);m.addAttribute("status",status);return "staff/rooms";}
    @PostMapping("/rooms/{id}/clean") public String clean(@PathVariable Long id,RedirectAttributes f){
        try{catalog.clean(id);f.addFlashAttribute("success","Phòng đã sẵn sàng nhận khách.");}
        catch(BusinessException e){f.addFlashAttribute("errorMessage",e.getMessage());}
        return "redirect:/staff/rooms";
    }
    @GetMapping("/audit") public String audit(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model m){m.addAttribute("result",query.audit(q,page));m.addAttribute("q",q);return "staff/audit";}
    @GetMapping("/invoices") public String invoices(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String state,@RequestParam(defaultValue="0") int page,Model m){m.addAttribute("result",query.invoices(q,state,page));m.addAttribute("q",q);m.addAttribute("state",state);return "staff/invoices";}
}
