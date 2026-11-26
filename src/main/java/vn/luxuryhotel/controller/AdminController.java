package vn.luxuryhotel.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.luxuryhotel.service.*;
import vn.luxuryhotel.dto.Forms;
import vn.luxuryhotel.entity.*;
import vn.luxuryhotel.exception.*;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Controller @RequiredArgsConstructor @RequestMapping("/admin")
public class AdminController {
    private final QueryService query;private final CatalogService catalog;private final AccountService accounts;
    @GetMapping({"","/"}) public String home(){return "redirect:/staff";}
    @GetMapping("/catalog/{kind}") public String catalog(@PathVariable String kind,Model m){
        Object rows=switch(kind){case "types"->query.catalog(RoomType.class);case "rooms"->query.catalog(Room.class);case "amenities"->query.catalog(Amenity.class);case "services"->query.catalog(HotelService.class);default->throw new NotFoundException();};
        m.addAttribute("rows",rows);m.addAttribute("kind",kind);return "admin/catalog";
    }
    @GetMapping("/catalog/{kind}/edit") public String edit(@PathVariable String kind,@RequestParam(required=false) Long id,Model m){
        Object form;
        switch(kind){
            case "types" -> {var f=new Forms.Type();if(id!=null){var t=query.get(RoomType.class,id);BeanUtils.copyProperties(t,f);f.setAmenityIds(t.getAmenities().stream().map(Amenity::getId).collect(Collectors.toSet()));}form=f;}
            case "rooms" -> {var f=new Forms.Room();if(id!=null){var r=query.get(Room.class,id);BeanUtils.copyProperties(r,f);f.setTypeId(r.getRoomType().getId());f.setStatus(r.getStatus().name());}form=f;}
            case "amenities" -> {var f=new Forms.Amenity();if(id!=null)BeanUtils.copyProperties(query.get(Amenity.class,id),f);form=f;}
            case "services" -> {var f=new Forms.Service();if(id!=null)BeanUtils.copyProperties(query.get(HotelService.class,id),f);form=f;}
            default -> throw new NotFoundException();
        }
        m.addAttribute("form",form);editModel(kind,m);return "admin/edit";
    }
    private void editModel(String kind,Model m){m.addAttribute("kind",kind);m.addAttribute("types",query.catalog(RoomType.class));m.addAttribute("amenities",query.catalog(Amenity.class));}
    private String saved(RedirectAttributes f,String kind){f.addFlashAttribute("success","Đã lưu dữ liệu.");return "redirect:/admin/catalog/"+kind;}
    @PostMapping("/catalog/types") public String types(@Valid @ModelAttribute("form") Forms.Type form,BindingResult errors,Model m,RedirectAttributes f){
        if(!errors.hasErrors())try{catalog.type(form);return saved(f,"types");}catch(BusinessException e){errors.reject("business",e.getMessage());}editModel("types",m);return "admin/edit";
    }
    @PostMapping("/catalog/rooms") public String rooms(@Valid @ModelAttribute("form") Forms.Room form,BindingResult errors,Model m,RedirectAttributes f){
        if(!errors.hasErrors())try{catalog.room(form);return saved(f,"rooms");}catch(BusinessException e){errors.reject("business",e.getMessage());}editModel("rooms",m);return "admin/edit";
    }
    @PostMapping("/catalog/amenities") public String amenities(@Valid @ModelAttribute("form") Forms.Amenity form,BindingResult errors,Model m,RedirectAttributes f){
        if(!errors.hasErrors())try{catalog.amenity(form);return saved(f,"amenities");}catch(BusinessException e){errors.reject("business",e.getMessage());}editModel("amenities",m);return "admin/edit";
    }
    @PostMapping("/catalog/services") public String services(@Valid @ModelAttribute("form") Forms.Service form,BindingResult errors,Model m,RedirectAttributes f){
        if(!errors.hasErrors())try{catalog.service(form);return saved(f,"services");}catch(BusinessException e){errors.reject("business",e.getMessage());}editModel("services",m);return "admin/edit";
    }
    @GetMapping("/users") public String users(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model m){m.addAttribute("result",query.users(q,page));m.addAttribute("q",q);return "admin/users";}
    @PostMapping("/users") public String user(@Valid @ModelAttribute Forms.UserEdit form,BindingResult errors,RedirectAttributes f){
        if(errors.hasErrors())throw new BusinessException("Thông tin quyền tài khoản không hợp lệ.");accounts.updateUser(form);f.addFlashAttribute("success","Đã cập nhật quyền tài khoản.");return "redirect:/admin/users";
    }
    @GetMapping("/revenue") public String revenue(@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(defaultValue="false") boolean demo,@RequestParam(defaultValue="0") int page,Model m){m.addAllAttributes(query.revenue(from,to,demo,page));return "admin/revenue";}
}
