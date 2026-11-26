package vn.luxuryhotel.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.luxuryhotel.entity.*;
import vn.luxuryhotel.repository.UserRepository;
@Service @RequiredArgsConstructor
public class ActorService {
    private final UserRepository users;
    public UserAccount current() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null) throw new AccessDeniedException("Vui lòng đăng nhập.");
        var user=users.findByEmail(auth.getName()).orElseThrow(()->new AccessDeniedException("Vui lòng đăng nhập."));
        if(!user.isEnabled()) throw new AccessDeniedException("Tài khoản đã khóa.");
        return user;
    }
    public UserAccount staff(){var u=current(); if(!isStaff(u))throw new AccessDeniedException("Chỉ dành cho lễ tân.");return u;}
    public UserAccount admin(){var u=current();if(!u.getRole().getName().equals("ADMIN"))throw new AccessDeniedException("Chỉ dành cho quản trị.");return u;}
    public boolean isStaff(UserAccount u){return !u.getRole().getName().equals("CUSTOMER");}
    public void owns(Booking b){var u=current();if(!isStaff(u)&&!u.getId().equals(b.getCustomer().getId()))throw new AccessDeniedException("Không có quyền truy cập đơn này.");}
}
